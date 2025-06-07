package memorypolicy;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedList;
import java.util.Queue;

public class DrawPanel extends JPanel {
    private CorePolicy core;
    private int windowSize;
    private int dataLength;

    public void setCoreData(CorePolicy core, int windowSize, int dataLength) {
        this.core = core;
        this.windowSize = windowSize;
        this.dataLength = dataLength;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (core == null) return;

        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        Queue<Character> pseudoQueue = new LinkedList<>();
        int gridSize = 30;
        int gridSpace = 5;

        for (int i = 0; i < dataLength; i++) {
            int pseudoCursor = core.getPageHistory().get(i).loc;
            char data = core.getPageHistory().get(i).data;
            Page.STATUS status = core.getPageHistory().get(i).status;

            switch (status) {
                case PAGEFAULT -> pseudoQueue.add(data);
                case MIGRATION -> {
                    if (!pseudoQueue.isEmpty()) pseudoQueue.poll();
                    pseudoQueue.add(data);
                }
            }

            for (int j = 0; j <= windowSize; j++) {
                int x = i * (gridSize + gridSpace);
                int y = j * gridSize;
                if (j == 0) {
                    drawGridText(g2d, x, y, gridSize, data);
                } else {
                    drawGrid(g2d, x, y, gridSize);
                }
            }

            int highlightX = i * (gridSize + gridSpace);
            int highlightY = (pseudoCursor) * gridSize;
            drawGridHighlight(g2d, highlightX, highlightY, gridSize, status);

            int depth = 1;
            for (char c : pseudoQueue) {
                int tx = i * (gridSize + gridSpace);
                int ty = depth * gridSize;
                drawGridText(g2d, tx, ty, gridSize, c);
                depth++;
            }
        }
    }

    private void drawGrid(Graphics2D g, int x, int y, int size) {
        g.setColor(Color.WHITE);
        g.drawRect(x, y, size, size);
    }

    private void drawGridHighlight(Graphics2D g, int x, int y, int size, Page.STATUS status) {
        Color color = switch (status) {
            case HIT -> Color.GREEN;
            case MIGRATION -> new Color(128, 0, 128);
            case PAGEFAULT -> Color.RED;
        };
        g.setColor(color);
        g.fillRect(x, y, size, size);
    }

    private void drawGridText(Graphics2D g, int x, int y, int size, char value) {
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.charWidth(value);
        int textHeight = fm.getAscent();
        int tx = x + (size - textWidth) / 2;
        int ty = y + (size + textHeight) / 2 - 4;
        g.drawString(String.valueOf(value), tx, ty);
    }
}