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

# Iterate through each file (row) and print the required details.
# for (file, hash_val), group in df.groupby(["Changed File", "Commit ID"]):
#     print("File:", file, "| Commit hash:", hash_val)
#     # Iterate over each row in the group
#     for index, row in group.iterrows():
#         print("Approach:", row["Approach"])
#         print(f"Len Aggregated_Src_SootOk: {len(row['Aggregated_Src_SootOk'])} -> {row['Aggregated_Src_SootOk']}")
#         print(f"Len Aggregated_Dest_SootOk: {len(row['Aggregated_Dest_SootOk'])} -> {row['Aggregated_Dest_SootOk']}")
#         print("New line change count:", len(row["Aggregated_Src_SootOk"]) + len(row["Aggregated_Dest_SootOk"]))
#         print("Vanilla line change count:", row["Total Changed Lines (Excl. everything non soot)"])
#         print("-----")



### TODO: need to investigate why new and vanilla counts seem to differ for some files... bit confusing.


results = []

for (file, commit), group in df.groupby(["Changed File", "Commit ID"]):
    baseline = group[group["Approach"] == "GumTree"]
    baseline_row = baseline.iloc[0]
    # TODO: this is primitive, should prob consider source and destination separately. will give me worse results.
    baseline_lines = set(baseline_row["Aggregated_Src_SootOk"]) | set(baseline_row["Aggregated_Dest_SootOk"])
    baseline_src = set(baseline_row["Aggregated_Src_SootOk"])
    baseline_dest = set(baseline_row["Aggregated_Dest_SootOk"])
    
    # now compare for each other approach in the group
    for idx, row in group.iterrows():
        if row["Approach"] == "GumTree":
            continue  # skip the baseline itself
        # getting the PDG approach's union of SootOk lines
        approach_lines = set(row["Aggregated_Src_SootOk"]) | set(row["Aggregated_Dest_SootOk"])
        approach_src = set(row["Aggregated_Src_SootOk"])
        approach_dest = set(row["Aggregated_Dest_SootOk"])


        ## TODO: inspect there things.
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

# conver the results into a DataFrame for further analysis.
diff_df = pd.DataFrame(results)

# agg summary statistics per approach.
summary = diff_df.groupby("Approach").agg({
    "Misses": ["mean", "sum"],
    "Hallucinations": ["mean", "sum"]
})
print("\nSummary statistics by approach:")
print(summary)

print(diff_df[["Misses", "Hallucinations"]].describe())
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

    print(f"-- {approach} --")
    print(f"Count (rows)              : {count}")
    print(f"Mean Abs Error (Misses)   : {mean_miss:.2f}")
    print(f"Median Abs Error (Misses) : {median_miss:.2f}")
    print(f"80th pct Abs Error (Misses): {pct80_miss:.2f}")
    print(f"90th pct Abs Error (Misses): {pct90_miss:.2f}")
    print(f"Mean Abs Error (Halluc)   : {mean_halluc:.2f}") 
    print(f"Median Hallucinations     : {median_halluc:.2f}")
    print(f"80th pct Hallucinations    : {pct80_halluc:.2f}")
    print(f"90th pct Hallucinations    : {pct90_halluc:.2f}")

    print("")


#### PLOTS 


approaches = sorted(diff_df["Approach"].unique())

# Prepare data for boxplots.
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
sns.violinplot(data=diff_df, x="Approach", y="Hallucinations", inner="quartile",  hue="Approach", palette="coolwarm", cut=0)
plt.title("Hallucinations Distribution by Approach")
plt.xticks(rotation=45)

plt.tight_layout()
plt.savefig("plots/violin.png")
plt.show()

# --- Grouped bar charts for median, etc 80th, and 90th percentiles ---

percentiles = np.arange(0, 101)

# plot percentiles for Misses.
plt.figure(figsize=(12, 6))
for approach in sorted(diff_df["Approach"].unique()):
    data = diff_df[diff_df["Approach"] == approach]["Misses"]
    # Compute percentile values for this approach.
    perc_values = np.percentile(data, percentiles)
    sns.lineplot(x=percentiles, y=perc_values, label=approach)
plt.xlabel("Percentile")
plt.ylabel("Misses")
plt.title("Percentile Curve for Misses by Approach")
plt.legend()
plt.grid(True)
plt.savefig("plots/misses.png")

# plot percentiles for Hallucinations.
plt.figure(figsize=(12, 6))
for approach in sorted(diff_df["Approach"].unique()):
    data = diff_df[diff_df["Approach"] == approach]["Hallucinations"]
    # cmp percentile values for this approach.
    perc_values = np.percentile(data, percentiles)
    sns.lineplot(x=percentiles, y=perc_values, label=approach)
plt.xlabel("Percentile")
plt.ylabel("Hallucinations")
plt.title("Percentile Curve for Hallucinations by Approach")
plt.legend()
plt.grid(True)
plt.savefig("plots/hallucinations.png")