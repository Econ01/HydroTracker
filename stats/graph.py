import argparse
import os
import json
import sys
import requests
import matplotlib.pyplot as plt
import matplotlib.dates as mdates
import matplotlib.colors as mcolors
from matplotlib.patches import Polygon
import numpy as np
from datetime import datetime, timedelta
import random

# Configuration
HISTORY_FILE = 'stats/download_history.json'
DEFAULT_OUTPUT = 'stats/overview.png'

def parse_args():
    parser = argparse.ArgumentParser(description="Generate GitHub Stars and Downloads graphs.")
    parser.add_argument('--repo', help="GitHub repository (owner/repo). Defaults to GITHUB_REPOSITORY env var.")
    parser.add_argument('--token', help="GitHub Personal Access Token. Defaults to GITHUB_TOKEN env var.")
    parser.add_argument('--mock', action='store_true', help="Use mock data instead of API calls.")
    parser.add_argument('--output', default=DEFAULT_OUTPUT, help=f"Output image file path (default: {DEFAULT_OUTPUT})")
    return parser.parse_args()

def get_headers(token):
    headers = {'Accept': 'application/vnd.github.v3.star+json'}
    if token:
        headers['Authorization'] = f'token {token}'
    return headers

def fetch_stars_history(repo, token):
    """
    Fetches stargazers to build a history of stars over time.
    Returns a list of (datetime, cumulative_count) tuples.
    """
    print(f"Fetching star history for {repo}...")
    url = f"https://api.github.com/repos/{repo}/stargazers"
    headers = get_headers(token)
    stars_data = []
    page = 1
    per_page = 100 
    
    while True:
        params = {'per_page': per_page, 'page': page}
        try:
            r = requests.get(url, headers=headers, params=params)
            r.raise_for_status()
            data = r.json()
        except requests.exceptions.RequestException as e:
            print(f"Error fetching stars: {e}")
            sys.exit(1)

        if not data:
            break

        for star in data:
            starred_at = datetime.strptime(star['starred_at'], '%Y-%m-%dT%H:%M:%SZ')
            stars_data.append(starred_at)

        # Safety break for very large repos to avoid hitting API limits blindly during testing
        if len(data) < per_page:
            break
        page += 1
        
        print(f"Fetched {len(stars_data)} stars...")

    print(f"Total stars fetched: {len(stars_data)}")
    
    stars_data.sort()
    
    # Create cumulative counts
    history = []
    for i, date in enumerate(stars_data):
        history.append((date, i + 1))
        
    return history

def fetch_total_downloads(repo, token):
    """
    Fetches the current total download count across all releases.
    """
    print(f"Fetching release data for {repo}...")
    url = f"https://api.github.com/repos/{repo}/releases"
    headers = get_headers(token)
    total_downloads = 0
    
    page = 1
    per_page = 100
    
    while True:
        params = {'per_page': per_page, 'page': page}
        try:
            r = requests.get(url, headers=headers, params=params)
            r.raise_for_status()
            data = r.json()
        except requests.exceptions.RequestException as e:
            print(f"Error fetching releases: {e}")
            sys.exit(1)

        if not data:
            break

        for release in data:
            for asset in release.get('assets', []):
                total_downloads += asset.get('download_count', 0)
        
        if len(data) < per_page:
            break
        page += 1

    print(f"Current total downloads: {total_downloads}")
    return total_downloads

def update_download_history(current_downloads):
    """
    Updates the local JSON file with the current timestamp and download count.
    Returns the full history list.
    """
    history = []
    if os.path.exists(HISTORY_FILE):
        try:
            with open(HISTORY_FILE, 'r') as f:
                history = json.load(f)
        except json.JSONDecodeError:
            print(f"Warning: Could not decode {HISTORY_FILE}, starting fresh.")

    # Append current status
    now_str = datetime.now().isoformat()
    history.append({'date': now_str, 'count': current_downloads})

    # Ensure directory exists
    os.makedirs(os.path.dirname(HISTORY_FILE), exist_ok=True)
    
    with open(HISTORY_FILE, 'w') as f:
        json.dump(history, f, indent=2)
    
    return history

def process_download_data(history, first_star_date):
    """
    Prepares download data for plotting.
    If history is sparse, extrapolates from first_star_date (count=0).
    """
    dates = []
    counts = []
    
    # Parse history
    for entry in history:
        dt = datetime.fromisoformat(entry['date'])
        dates.append(dt)
        counts.append(entry['count'])

    if dates:
        earliest_record = min(dates)
        if first_star_date and earliest_record > first_star_date:
            dates.insert(0, first_star_date)
            counts.insert(0, 0)
    elif first_star_date:
        dates = [first_star_date]
        counts = [0]
            
    return dates, counts

def generate_mock_data():
    print("Generating MOCK data...")
    # Mock Stars
    start_date = datetime.now() - timedelta(days=365)
    stars_history = []
    current_stars = 0
    for i in range(365):
        if random.random() > 0.5:
            current_stars += random.randint(0, 5)
        date = start_date + timedelta(days=i)
        stars_history.append((date, current_stars))
    
    # Mock Downloads
    download_dates = [start_date, datetime.now()]
    download_counts = [0, random.randint(1000, 5000)]
    
    return stars_history, (download_dates, download_counts)

def plot_graphs(stars_data, download_data, output_file):
    # Unpack data
    star_dates, star_counts = zip(*stars_data) if stars_data else ([], [])
    download_dates, download_counts = download_data

    # Swap order: Downloads first (ax1), Stars second (ax2)
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(12, 5))
    
    # Make background transparent
    fig.patch.set_alpha(0.0)
    
    # Styling helpers
    def style_axis(ax):
        ax.set_ylabel("")
        ax.tick_params(axis='both', which='both', labelbottom=False, labelleft=False)
        ax.grid(True, linestyle='--', alpha=0.3)
        ax.spines['top'].set_visible(False)
        ax.spines['right'].set_visible(False)
        ax.spines['bottom'].set_visible(True)
        ax.spines['left'].set_visible(True)
        # Make axis background transparent
        ax.set_facecolor('none')

    def gradient_fill(ax, dates, counts, color):
        if not dates or not counts:
            return

        x_num = mdates.date2num(dates)
        y = np.array(counts)
        
        line, = ax.plot(dates, counts, color=color, linewidth=2)
        
        rgb = mcolors.to_rgb(color)
        
        # Gradient logic (0=Transparent at Bottom, 1=Color at Top)
        z = np.linspace(0, 1, 100).reshape(-1, 1)
        
        cdict = {
            'red':   [(0.0, rgb[0], rgb[0]), (1.0, rgb[0], rgb[0])],
            'green': [(0.0, rgb[1], rgb[1]), (1.0, rgb[1], rgb[1])],
            'blue':  [(0.0, rgb[2], rgb[2]), (1.0, rgb[2], rgb[2])],
            'alpha': [(0.0, 0.0, 0.0),       (1.0, 0.5, 0.5)]
        }
        custom_cmap = mcolors.LinearSegmentedColormap('custom_gradient', cdict)
        
        xmin, xmax = min(x_num), max(x_num)
        # Use full range of Y for the gradient
        ymin, ymax = 0, max(y) if max(y) > 0 else 1
        
        im = ax.imshow(z, aspect='auto', extent=[xmin, xmax, 0, ymax], 
                       cmap=custom_cmap, origin='lower', zorder=line.get_zorder()-1)
        
        verts = [(x_num[0], 0)] + list(zip(x_num, y)) + [(x_num[-1], 0)]
        poly = Polygon(verts, transform=ax.transData)
        im.set_clip_path(poly)

    def annotate_last_point(ax, dates, counts, color):
        if not dates or not counts:
            return
        last_date = dates[-1]
        last_count = counts[-1]
        
        ax.plot(last_date, last_count, 'o', color=color, markersize=8)
        
        ax.annotate(f"{last_count:,}", 
                    xy=(last_date, last_count), 
                    xytext=(0, 10), 
                    textcoords='offset points', 
                    ha='center', 
                    va='bottom', 
                    fontsize=12, 
                    fontweight='bold', 
                    color=color)

    # Helper to add padding
    def add_padding(ax, dates, counts):
        if not dates or not counts:
            return
        
        # X Padding
        x_start = mdates.date2num(dates[0])
        x_end = mdates.date2num(dates[-1])
        x_span = x_end - x_start
        # Add 5% padding to the right
        ax.set_xlim(right=mdates.num2date(x_end + x_span * 0.05))
        
        # Y Padding
        y_max = max(counts)
        # Add 10% padding to the top
        ax.set_ylim(top=y_max * 1.10, bottom=0)

    # Plot Downloads
    color_downloads = '#008080'
    if download_dates:
        style_axis(ax1)
        gradient_fill(ax1, download_dates, download_counts, color_downloads)
        annotate_last_point(ax1, download_dates, download_counts, color_downloads)
        add_padding(ax1, download_dates, download_counts)
    else:
        ax1.text(0.5, 0.5, "No Data", ha='center', va='center')
    
    ax1.set_title("Total Downloads", fontsize=14, fontweight='bold', color='#333333')

    # Plot Stars
    color_stars = '#800000'
    if star_dates:
        style_axis(ax2)
        gradient_fill(ax2, star_dates, star_counts, color_stars)
        annotate_last_point(ax2, star_dates, star_counts, color_stars)
        add_padding(ax2, star_dates, star_counts)
    else:
        ax2.text(0.5, 0.5, "No Data", ha='center', va='center')

    ax2.set_title("Stars", fontsize=14, fontweight='bold', color='#333333')

    plt.tight_layout()
    
    os.makedirs(os.path.dirname(output_file), exist_ok=True)
    # Transparency: Savefig
    plt.savefig(output_file, dpi=100, bbox_inches='tight', transparent=True)
    print(f"Graph saved to {output_file}")

def main():
    args = parse_args()
    
    if args.mock:
        stars_history, download_data = generate_mock_data()
    else:
        repo = args.repo or os.environ.get('GITHUB_REPOSITORY')
        token = args.token or os.environ.get('GITHUB_TOKEN')
        
        if not repo:
            print("Error: Repository not provided. Use --repo or set GITHUB_REPOSITORY.")
            sys.exit(1)
            
        stars_history = fetch_stars_history(repo, token)
        current_downloads = fetch_total_downloads(repo, token)
        raw_history = update_download_history(current_downloads)
        first_star_date = stars_history[0][0] if stars_history else datetime.now()
        download_data = process_download_data(raw_history, first_star_date)

    plot_graphs(stars_history, download_data, args.output)

if __name__ == "__main__":
    main()
