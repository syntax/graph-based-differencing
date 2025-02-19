import pandas as pd
import numpy as np


def load_data(csv_path):
    return pd.read_csv(csv_path)

def prepare_data(df):
    rename_map = {
        "Changed File": "changed_file",
        "Approach": "approach",
        "Total Changed Lines (Excl. everything non soot)": "lines_excl_nonsoot",
        "Project": "project",
        "Commit ID": "commit",
    }
    df = df.rename(columns=rename_map)
    df["lines_excl_nonsoot"] = pd.to_numeric(df["lines_excl_nonsoot"], errors='coerce').fillna(0)
    return df

def merge_approaches(df):
    key_cols = ["project", "commit", "changed_file"]

    gumtree = df[df["approach"] == "GumTree"]
    vf2     = df[df["approach"] == "PDGdiff-VF2"]
    ged     = df[df["approach"] == "PDGdiff-GED"]

    gumtree = gumtree[key_cols + ["lines_excl_nonsoot"]]\
              .rename(columns={"lines_excl_nonsoot": "gumtree_lines"})
    vf2     = vf2[key_cols + ["lines_excl_nonsoot"]]\
              .rename(columns={"lines_excl_nonsoot": "vf2_lines"})
    ged     = ged[key_cols + ["lines_excl_nonsoot"]]\
              .rename(columns={"lines_excl_nonsoot": "ged_lines"})

    merged = gumtree.merge(vf2, on=key_cols, how="left")\
                    .merge(ged, on=key_cols, how="left")

    # Fill missing if no row found for that approach
    merged["vf2_lines"] = merged["vf2_lines"].fillna(0)
    merged["ged_lines"] = merged["ged_lines"].fillna(0)
    return merged

def smooth_alpha5_rel_error(gt, pred, alpha=5):
    """
    Relative error = |pred - gt| / max(gt, alpha).
    By default, alpha=5.
    """
    return abs(pred - gt) / max(gt, alpha)

def compute_metrics(df, alpha=5):
    """
    - Absolute error
    - Our chosen 'smooth alpha=5' relative error
    - Bounded relative error (optional)
    - Macro aggregator
    """
    df = df.copy()
    
    # abs err
    df["error_vf2"] = (df["vf2_lines"] - df["gumtree_lines"]).abs()
    df["error_ged"] = (df["ged_lines"] - df["gumtree_lines"]).abs()

    # rel err
    df["rel_smooth_vf2"] = df.apply(
        lambda row: smooth_alpha5_rel_error(row["gumtree_lines"], row["vf2_lines"], alpha=alpha), 
        axis=1
    )
    df["rel_smooth_ged"] = df.apply(
        lambda row: smooth_alpha5_rel_error(row["gumtree_lines"], row["ged_lines"], alpha=alpha),
        axis=1
    )

    return df

def summarize(df, approach="vf2"):
    """
    Summarize the final metrics:
      - Mean/median/P80/P90 absolute error
      - Mean "smooth alpha=5" relative error
      - Macro error
      - Pearson correlation
    """
    if approach == "vf2":
        error_col   = "error_vf2"
        rel_col     = "rel_smooth_vf2"
        approach_lines_col = "vf2_lines"
    else:
        error_col   = "error_ged"
        rel_col     = "rel_smooth_ged"
        approach_lines_col = "ged_lines"

    mae       = df[error_col].mean()
    median_ae = df[error_col].median()
    p80_ae    = df[error_col].quantile(0.80)
    p90_ae    = df[error_col].quantile(0.90)

    mean_rel  = df[rel_col].mean()

    # macro aggregator
    sum_err   = df[error_col].sum()
    sum_gt    = df["gumtree_lines"].sum()
    if sum_gt == 0:
        # a;; GT are zero
        sum_pred = df[approach_lines_col].sum()
        macro = 0.0 if sum_pred == 0 else 1.0
    else:
        macro = sum_err / sum_gt

    # Pearson corr
    corr_mtx = df[[approach_lines_col, "gumtree_lines"]].corr(method="pearson")
    corr = corr_mtx.iat[0,1]  # might be NaN if no variation

    return {
        "count": len(df),
        "mean_abs_error": mae,
        "median_abs_error": median_ae,
        "p80_abs_error": p80_ae,
        "p90_abs_error": p90_ae,
        "mean_rel_error": mean_rel,
        "macro_agg_error": macro,
        "pearson_corr": corr
    }

def main():
    csv_path = "diff_results.csv"
    df = load_data(csv_path)
    df = prepare_data(df)
    merged = merge_approaches(df)

    # using a5 (increasing further drops err, but i think more than 5 is cheeky)
    alpha = 5
    results = compute_metrics(merged, alpha=alpha)

    # Summaries
    stats_vf2 = summarize(results, approach="vf2")
    stats_ged = summarize(results, approach="ged")

    # Print
    print(format_stats("PDGdiff-VF2", stats_vf2))
    print(format_stats("PDGdiff-GED", stats_ged))

def format_stats(label, st):
    return (
        f"\n-- {label} --\n"
        f"Count (rows)              : {st['count']}\n"
        f"Mean Abs Error            : {st['mean_abs_error']:.2f}\n"
        f"Median Abs Error          : {st['median_abs_error']:.2f}\n"
        f"80th pct Abs Error        : {st['p80_abs_error']:.2f}\n"
        f"90th pct Abs Error        : {st['p90_abs_error']:.2f}\n"
        f"Mean (Smooth a=5) Rel Err : {st['mean_rel_error']:.3f}\n"
        f"Macro-Agg Error           : {st['macro_agg_error']:.3f}\n"
        f"Pearson Corr w/ GumTree   : {st['pearson_corr']:.3f}\n"
    )

if __name__ == "__main__":
    main()
