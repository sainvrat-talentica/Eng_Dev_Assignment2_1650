#!/usr/bin/env python3
"""Generate deliverables/architecture-diagram.png for the Assignment 2 write-up."""

from pathlib import Path

try:
    import matplotlib.pyplot as plt
    from matplotlib.patches import FancyBboxPatch, FancyArrowPatch
except ImportError:
    raise SystemExit("Install matplotlib: pip install matplotlib")

A2_ROOT = Path(__file__).resolve().parents[1]
OUT = A2_ROOT / "deliverables" / "architecture-diagram.png"

BOXES = [
    (0.5, 0.88, "Sample CSV files\n(orders, warehouse, fleet,\nfeedback, external factors)"),
    (0.5, 0.72, "SampleDataImportService\n→ PostgreSQL analytics.* schema"),
    (0.5, 0.56, "AnalyticsOrderRepository\n(SQL joins & aggregations)"),
    (0.5, 0.40, "CorrelationEngine\n(rule-based multi-domain matching)"),
    (0.5, 0.24, "InsightGenerator\n(narratives + recommendations)"),
    (0.5, 0.08, "REST API (:8080) / CLI / Dashboard (:3002)"),
]

BOX_W, BOX_H = 0.72, 0.11


def main() -> None:
    fig, ax = plt.subplots(figsize=(8, 10))
    ax.set_xlim(0, 1)
    ax.set_ylim(0, 1)
    ax.axis("off")
    ax.set_title(
        "SwiftEats — Delivery Failure Root-Cause Analytics Pipeline",
        fontsize=14,
        fontweight="bold",
        pad=16,
    )

    centers = []
    for x, y, label in BOXES:
        box = FancyBboxPatch(
            (x - BOX_W / 2, y - BOX_H / 2),
            BOX_W,
            BOX_H,
            boxstyle="round,pad=0.012,rounding_size=0.02",
            linewidth=1.2,
            edgecolor="#1f4e79",
            facecolor="#e8f1fb",
        )
        ax.add_patch(box)
        ax.text(x, y, label, ha="center", va="center", fontsize=9.5, color="#102a43")
        centers.append((x, y))

    for i in range(len(centers) - 1):
        x1, y1 = centers[i]
        x2, y2 = centers[i + 1]
        arrow = FancyArrowPatch(
            (x1, y1 - BOX_H / 2 - 0.005),
            (x2, y2 + BOX_H / 2 + 0.005),
            arrowstyle="-|>",
            mutation_scale=14,
            linewidth=1.4,
            color="#1f4e79",
        )
        ax.add_patch(arrow)

    OUT.parent.mkdir(parents=True, exist_ok=True)
    fig.savefig(OUT, dpi=150, bbox_inches="tight", facecolor="white")
    plt.close(fig)
    print(f"Wrote {OUT}")


if __name__ == "__main__":
    main()
