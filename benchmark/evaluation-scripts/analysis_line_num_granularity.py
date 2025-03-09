import pandas as pd
import ast
import matplotlib.pyplot as plt
import numpy as np
import seaborn as sns

df = pd.read_csv("diff_results_gumtree_indiv_line_nums.csv")

src_del = 'Deleted Lines (Src) (SootOK)'
src_upd = 'Updated Lines (Src) (SootOk)'
src_move = 'Moved Lines (Src) (SootOk)'

dest_ins = 'Inserted Lines (Dst) (SootOK)'
dest_upd = 'Updated Lines (Dst) (SootOk)'
dest_move = 'Moved Lines (Dst) (SootOk)'

# helper function to safely convert a string representation of a list into an actual list
def parse_list(cell):
    if pd.isna(cell) or cell == "":
        return []
    try:
        return ast.literal_eval(cell) if isinstance(cell, str) else cell
    except Exception:
        return []

for col in [src_del, src_upd, src_move, dest_ins, dest_upd, dest_move]:
    df[col] = df[col].apply(parse_list)

def aggregate_sootok(row):
    # for gumtree, we need to consider moved lines as well, for pdg we dont.
    if row["Approach"] == "GumTree":
        src = row[src_del] + row[src_upd] + row[src_move]
        dest = row[dest_ins] + row[dest_upd] + row[dest_move]
    else:
        src = row[src_del] + row[src_upd]
        dest = row[dest_ins] + row[dest_upd]
    return pd.Series({
        'Aggregated_Src_SootOk': sorted(set(src)),
        'Aggregated_Dest_SootOk': sorted(set(dest))
    })

df[['Aggregated_Src_SootOk', 'Aggregated_Dest_SootOk']] = df.apply(aggregate_sootok, axis=1)

results = []

for (file, commit), group in df.groupby(["Changed File", "Commit ID"]):
    baseline = group[group["Approach"] == "GumTree"]
    if baseline.empty:
        continue
    baseline_row = baseline.iloc[0]

    # Union of lines for baseline approach
    baseline_lines = set(baseline_row["Aggregated_Src_SootOk"]) | set(baseline_row["Aggregated_Dest_SootOk"])
    baseline_src = set(baseline_row["Aggregated_Src_SootOk"])
    baseline_dest = set(baseline_row["Aggregated_Dest_SootOk"])
    
    for idx, row in group.iterrows():
        if row["Approach"] == "GumTree":
            continue  # skip the baseline itself
        
        # union of lines for the current approach
        approach_lines = set(row["Aggregated_Src_SootOk"]) | set(row["Aggregated_Dest_SootOk"])
        approach_src = set(row["Aggregated_Src_SootOk"])
        approach_dest = set(row["Aggregated_Dest_SootOk"])

        misses_src = baseline_src - approach_src
        misses_dest = baseline_dest - approach_dest
        hallucinations_src = approach_src - baseline_src
        hallucinations_dest = approach_dest - baseline_dest

        # lines that GumTree reports but the approach does not: MISS
        misses = baseline_lines - approach_lines
        # lines that the approach reports but are not in GumTree: HALLUCINATIONS
        hallucinations = approach_lines - baseline_lines

        results.append({
            "Changed File": file,
            "Commit ID": commit,
            "Approach": row["Approach"],
            "GumTree_Count": len(baseline_lines),
            "Approach_Count": len(approach_lines),
            "Misses": len(misses),
            "Hallucinations": len(hallucinations),
            "Misses_Src": len(misses_src),
            "Misses_Dest": len(misses_dest),
            "Hallucinations_Src": len(hallucinations_src),
            "Hallucinations_Dest": len(hallucinations_dest),
        })

diff_df = pd.DataFrame(results)

hybrid_rows = []
for (file, commit), group in diff_df.groupby(["Changed File", "Commit ID"]):
    pdg_vf2 = group[group["Approach"] == "PDGdiff-VF2"] # vf2
    pdg_ged = group[group["Approach"] == "PDGdiff-GED"] # ged
    if not pdg_vf2.empty and not pdg_ged.empty:
        vf2_err = pdg_vf2.iloc[0]["Misses"] + pdg_vf2.iloc[0]["Hallucinations"]
        ged_err = pdg_ged.iloc[0]["Misses"] + pdg_ged.iloc[0]["Hallucinations"]
        chosen_row = pdg_vf2.iloc[0] if vf2_err <= ged_err else pdg_ged.iloc[0]
        chosen_row = chosen_row.copy()
        chosen_row["Approach"] = "PDG-Hybrid"
        hybrid_rows.append(chosen_row)

hybrid_df = pd.DataFrame(hybrid_rows)
diff_df = pd.concat([diff_df, hybrid_df], ignore_index=True)

# agg summary statistics per approach
summary = diff_df.groupby("Approach").agg({
    "Misses": ["mean", "sum"],
    "Hallucinations": ["mean", "sum"]
})
print("\nSummary statistics by approach:")
print(summary)

print("\nOverall Misses/Hallucinations describe():")
print(diff_df[["Misses", "Hallucinations"]].describe())

# Just a sanity check (should be empty unless there's weird negative values)
print("\nRows with negative Misses or Hallucinations (should be empty):")
print(diff_df[(diff_df["Misses"] < 0) | (diff_df["Hallucinations"] < 0)])


for approach in diff_df["Approach"].unique():
    group = diff_df[diff_df["Approach"] == approach]
    count = group.shape[0]
    mean_miss = group["Misses"].mean()
    mean_halluc = group["Hallucinations"].mean()
    median_miss = group["Misses"].median()
    median_halluc = group["Hallucinations"].median()
    pct80_miss = group["Misses"].quantile(0.8)
    pct80_halluc = group["Hallucinations"].quantile(0.8)
    pct90_miss = group["Misses"].quantile(0.9)
    pct90_halluc = group["Hallucinations"].quantile(0.9)

    # cmp Pearson correlation with GumTree counts. TODO: arguably this is a bit primitive as we only take counts
    pearson_corr = group["Approach_Count"].corr(group["GumTree_Count"])

    print(f"-- {approach} --")
    print(f"Count (rows)               : {count}")
    print(f"Mean Abs Error (Misses)    : {mean_miss:.2f}")
    print(f"Median Abs Error (Misses)  : {median_miss:.2f}")
    print(f"80th pct Abs Error (Misses): {pct80_miss:.2f}")
    print(f"90th pct Abs Error (Misses): {pct90_miss:.2f}")
    print(f"Mean Abs Error (Halluc)    : {mean_halluc:.2f}")
    print(f"Median Hallucinations      : {median_halluc:.2f}")
    print(f"80th pct Hallucinations    : {pct80_halluc:.2f}")
    print(f"90th pct Hallucinations    : {pct90_halluc:.2f}")

    print(f"Pearson correlation with GumTree: {pearson_corr:.3f}" if pd.notna(pearson_corr) else 
          "Pearson correlation with GumTree: N/A (not enough variation or data points)")
    print("")


# --------------------------------------------------
# plots 
# --------------------------------------------------

approaches = sorted(diff_df["Approach"].unique())

# Prepare data for boxplots/violin plots.
data_misses = [diff_df[diff_df["Approach"] == app]["Misses"] for app in approaches]
data_halluc = [diff_df[diff_df["Approach"] == app]["Hallucinations"] for app in approaches]

plt.figure(figsize=(12, 6))

# Violin plot for Misses
plt.subplot(1, 2, 1)
sns.violinplot(data=diff_df, x="Approach", y="Misses", inner="quartile", hue="Approach", palette="coolwarm", cut=0)
plt.title("Misses Distribution by Approach")
plt.xticks(rotation=45)

# Violin plot for Hallucinations
plt.subplot(1, 2, 2)
sns.violinplot(data=diff_df, x="Approach", y="Hallucinations", inner="quartile", hue="Approach", palette="coolwarm", cut=0)
plt.title("Hallucinations Distribution by Approach")
plt.xticks(rotation=45)

plt.tight_layout()
plt.savefig("plots/violin.png", dpi=600, bbox_inches='tight')
# plt.show()

# --- PERCENTILES ---
percentiles = np.arange(0, 101)
tick_step = 5

plt.figure(figsize=(12, 6))
for approach in approaches:
    data = diff_df[diff_df["Approach"] == approach]["Misses"]
    perc_values = np.percentile(data, percentiles)
    sns.lineplot(x=percentiles, y=perc_values, label=approach)
plt.xlabel("Percentile")
plt.ylabel("Misses")
plt.title("Percentile Curve for Misses by Approach")
plt.legend()
ax = plt.gca()
y_max = diff_df["Misses"].max()
ax.set_yticks(np.arange(0, y_max + tick_step, tick_step))
plt.grid(True)
plt.savefig("plots/misses.png", dpi=600, bbox_inches='tight')

plt.figure(figsize=(12, 6))
for approach in approaches:
    data = diff_df[diff_df["Approach"] == approach]["Hallucinations"]
    perc_values = np.percentile(data, percentiles)
    sns.lineplot(x=percentiles, y=perc_values, label=approach)
plt.xlabel("Percentile")
plt.ylabel("Hallucinations")
plt.title("Percentile Curve for Hallucinations by Approach")
plt.legend()
ax = plt.gca()
y_max = diff_df["Hallucinations"].max()
ax.set_yticks(np.arange(0, y_max + tick_step, tick_step))
plt.grid(True)
plt.savefig("plots/hallucinations.png", dpi=600, bbox_inches='tight')

# --- INVESTIGATING GRANULARITY - source vs dest misses ---
summary_src_dest = diff_df.groupby("Approach").agg({
    "Misses_Src": "mean",
    "Misses_Dest": "mean",
    "Hallucinations_Src": "mean",
    "Hallucinations_Dest": "mean"
}).reset_index()

print("\nAverage Values by Approach (Source vs Destination):")
print(summary_src_dest)

approaches = summary_src_dest["Approach"]
x = np.arange(len(approaches))
width = 0.35

fig, ax = plt.subplots(figsize=(10,6))
bars_src = ax.bar(x - width/2, summary_src_dest["Misses_Src"], width, label="Source Misses")
bars_dest = ax.bar(x + width/2, summary_src_dest["Misses_Dest"], width, label="Destination Misses")

ax.set_ylabel("Average Misses (lines)")
ax.set_title("Average Misses by Approach: Source vs Destination")
ax.set_xticks(x)
ax.set_xticklabels(approaches)
ax.legend()
plt.tight_layout()
plt.savefig("plots/avg_misses.png", dpi=600, bbox_inches='tight')

fig, ax = plt.subplots(figsize=(10,6))
bars_src = ax.bar(x - width/2, summary_src_dest["Hallucinations_Src"], width, label="Source Hallucinations")
bars_dest = ax.bar(x + width/2, summary_src_dest["Hallucinations_Dest"], width, label="Destination Hallucinations")

ax.set_ylabel("Average Hallucinations (lines)")
ax.set_title("Average Hallucinations by Approach: Source vs Destination")
ax.set_xticks(x)
ax.set_xticklabels(approaches)
ax.legend()
plt.tight_layout()
plt.savefig("plots/avg_hallucinations.png", dpi=600, bbox_inches='tight')

# --------------------------------------------------
# ANALYSIS OF OPERATION TYPES AS A PERCENTAGE OF TOTAL CHANGED LINES
# (Using the SootOK columns, and excluding move operations for GED and VF2)
# --------------------------------------------------


all_op_cols = [src_del, dest_ins, src_upd, dest_upd, src_move, dest_move]
all_op_labels = ["Deleted (Src)", "Inserted (Dst)", "Updated (Src)", "Updated (Dst)", "Moved (Src)", "Moved (Dst)"]

# For GED and VF2, we exclude move columns. So define these lists:
non_move_op_cols = [src_del, dest_ins, src_upd, dest_upd]
non_move_op_labels = ["Deleted (Src)", "Inserted (Dst)", "Updated (Src)", "Updated (Dst)"]

# Helper: count total lines in a column (assuming each cell is a list)
def count_lines(series):
    return series.apply(lambda x: len(x) if isinstance(x, list) else 0).sum()

op_stats = []
for approach, group in df.groupby("Approach"):
    if approach in ["PDGdiff-GED", "PDGdiff-VF2"]:
        op_sum = {}
        for col, label in zip(non_move_op_cols, non_move_op_labels):
            op_sum[label] = count_lines(group[col])
        op_sum["Moved (Src)"] = 0
        op_sum["Moved (Dst)"] = 0
        total_lines = sum(op_sum.values())
        percentages = {label: (op_sum[label] / total_lines) * 100 if total_lines > 0 else 0 for label in all_op_labels}
    else:
        op_sum = {}
        for col, label in zip(all_op_cols, all_op_labels):
            op_sum[label] = count_lines(group[col])
        total_lines = sum(op_sum.values())
        percentages = {label: (op_sum[label] / total_lines) * 100 if total_lines > 0 else 0 for label in all_op_labels}

    record = {"Approach": approach, "Total_Lines": total_lines}
    record.update(percentages)
    op_stats.append(record)

op_stats_df = pd.DataFrame(op_stats).set_index("Approach").sort_index()

print("\n--- Operation Type Percentages by Approach (SootOK columns, excluding moves for GED/VF2) ---")
print(op_stats_df)

plt.figure(figsize=(10,6))
approaches = op_stats_df.index.tolist()
x = np.arange(len(approaches))
bottom = np.zeros(len(approaches))

for label in all_op_labels:
    perc = op_stats_df[label].values
    plt.bar(x, perc, bottom=bottom, label=label)
    bottom += perc

plt.xticks(x, approaches, rotation=45, ha='right')
plt.ylabel("Percentage (%)")
plt.title("Operation Types as Percentage of Total Changed Lines (excluding lines beyond the scope of Soot)")
plt.legend()
plt.tight_layout()
plt.savefig("plots/operation_types_percentage_stacked.png", dpi=600, bbox_inches='tight')
# plt.show()




operation_cols = [src_del, dest_ins, src_upd, dest_upd]
operation_labels = ["Deleted (Src)", "Inserted (Dst)", "Updated (Src)", "Updated (Dst)"]

# hlper: count total lines in a column (each cell is a list)
def count_lines(series):
    return series.apply(lambda x: len(x) if isinstance(x, list) else 0).sum()

# Calculate the total count of each operation type per approach
op_summary = df.groupby("Approach").apply(
    lambda group: pd.Series({
        label: count_lines(group[col])
        for label, col in zip(operation_labels, operation_cols)
    })
)

# cmp percentages for each operation type per approach
op_totals = op_summary.sum(axis=1)
op_percentages = op_summary.div(op_totals, axis=0) * 100

# plot a stacked bar chart for the operation type percentages (excluding moves)
fig, ax = plt.subplots(figsize=(10, 6))
approaches = op_percentages.index.tolist()
x = np.arange(len(approaches))
bottom = np.zeros(len(approaches))

for label in operation_labels:
    percentages = op_percentages[label].values
    ax.bar(x, percentages, bottom=bottom, label=label)
    bottom += percentages

ax.set_xlabel("Approach")
ax.set_ylabel("Percentage (%)")
ax.set_title("Operation Types as Percentage of Total Changed Lines (Excluding Moves)")
ax.set_xticks(x)
ax.set_xticklabels(approaches, rotation=45)
ax.legend(title="Operation Type")

plt.tight_layout()
plt.savefig("plots/percentage_operations_no_moves_all.png", dpi=600, bbox_inches='tight')
plt.show()