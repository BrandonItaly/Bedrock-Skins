package io.github.brandonitaly.bedrockskins.gui.widget;

/** Fits complete columns to the viewport while retaining each card's aspect ratio. */
record CardGridLayout(int columns, int cardWidth, int cardHeight, int remainder) {
    int columnX(int column, int gap) {
        return column * (cardWidth + gap) + Math.min(column, remainder);
    }

    static CardGridLayout fit(int available, int preferredWidth, int preferredHeight, int gap) {
        available = Math.max(1, available);
        int minimumWidth = Math.max(1, preferredWidth * 3 / 4);
        int columns = Math.max(1, Math.min(
            Math.round((available + gap) / (float) (preferredWidth + gap)),
            (available + gap) / (minimumWidth + gap)));
        int width = Math.max(1, (available - (columns - 1) * gap) / columns);
        int height = Math.max(1, Math.round(width * preferredHeight / (float) preferredWidth));
        int remainder = available - (columns * width + (columns - 1) * gap);
        return new CardGridLayout(columns, width, height, remainder);
    }
}
