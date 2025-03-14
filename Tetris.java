import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.io.IOException;
import java.util.Random;

public class Tetris extends JPanel implements ActionListener, KeyListener {
    private final int BOARD_WIDTH = 10;
    private final int BOARD_HEIGHT = 20;
    private final int TILE_SIZE = 30;
    private final int DELAY = 500; // Initial delay for falling
    private Timer timer;
    private boolean[][] board;
    private Shape currentShape;
    private int currentX, currentY;
    private int score = 0;
    private boolean isGameOver = false;
    private boolean isPaused = false;
    private Random random = new Random();

    // Colors for shapes
    private final Color[] COLORS = {
            Color.CYAN, Color.YELLOW, Color.MAGENTA, Color.ORANGE, Color.BLUE, Color.GREEN, Color.RED
    };

    // Background music
    private Clip backgroundMusic;

    public Tetris() {
        setPreferredSize(new Dimension(BOARD_WIDTH * TILE_SIZE, BOARD_HEIGHT * TILE_SIZE));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        board = new boolean[BOARD_HEIGHT][BOARD_WIDTH];
        timer = new Timer(DELAY, this);

        // Load and play background music
        try {
            File audioFile = new File("bg.wav"); // Ensure the path is correct
            AudioInputStream originalStream = AudioSystem.getAudioInputStream(audioFile);

            AudioFormat baseFormat = originalStream.getFormat();
            AudioFormat targetFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16, // Convert to 16-bit
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2, // Frame size (bytes per frame)
                    baseFormat.getSampleRate(),
                    false // Little-endian
            );

            // Convert the audio format
            AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, originalStream);

            // Load into Clip
            backgroundMusic = AudioSystem.getClip();
            backgroundMusic.open(convertedStream);
            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
            backgroundMusic.start();
        } catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
        }



        startGame();
    }

    private void startGame() {
        score = 0;
        isGameOver = false;
        isPaused = false;
        clearBoard();
        spawnShape();
        timer.start();
    }

    private void clearBoard() {
        for (int row = 0; row < BOARD_HEIGHT; row++) {
            for (int col = 0; col < BOARD_WIDTH; col++) {
                board[row][col] = false;
            }
        }
    }

    private void spawnShape() {
        currentShape = new Shape();
        currentX = BOARD_WIDTH / 2 - 1;
        currentY = 0;

        if (!isValidMove(currentShape, currentX, currentY)) {
            isGameOver = true;
            timer.stop();
        }
    }

    private boolean isValidMove(Shape shape, int x, int y) {
        for (int row = 0; row < shape.getHeight(); row++) {
            for (int col = 0; col < shape.getWidth(); col++) {
                if (shape.isFilled(row, col)) {
                    int newX = x + col;
                    int newY = y + row;

                    if (newX < 0 || newX >= BOARD_WIDTH || newY >= BOARD_HEIGHT || (newY >= 0 && board[newY][newX])) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void placeShape() {
        for (int row = 0; row < currentShape.getHeight(); row++) {
            for (int col = 0; col < currentShape.getWidth(); col++) {
                if (currentShape.isFilled(row, col)) {
                    board[currentY + row][currentX + col] = true;
                }
            }
        }
        clearLines();
        spawnShape();
    }

    private void clearLines() {
        int linesCleared = 0;
        for (int row = 0; row < BOARD_HEIGHT; row++) {
            boolean isLineFull = true;
            for (int col = 0; col < BOARD_WIDTH; col++) {
                if (!board[row][col]) {
                    isLineFull = false;
                    break;
                }
            }
            if (isLineFull) {
                linesCleared++;
                for (int r = row; r > 0; r--) {
                    System.arraycopy(board[r - 1], 0, board[r], 0, BOARD_WIDTH);
                }
                board[0] = new boolean[BOARD_WIDTH];
            }
        }
        score += linesCleared * 100;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isGameOver && !isPaused) {
            if (isValidMove(currentShape, currentX, currentY + 1)) {
                currentY++;
            } else {
                placeShape();
            }
            repaint();
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw the board
        for (int row = 0; row < BOARD_HEIGHT; row++) {
            for (int col = 0; col < BOARD_WIDTH; col++) {
                if (board[row][col]) {
                    g.setColor(COLORS[random.nextInt(COLORS.length)]);
                    g.fillRect(col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    g.setColor(Color.BLACK);
                    g.drawRect(col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
            }
        }

        // Draw the current shape
        if (currentShape != null) {
            for (int row = 0; row < currentShape.getHeight(); row++) {
                for (int col = 0; col < currentShape.getWidth(); col++) {
                    if (currentShape.isFilled(row, col)) {
                        g.setColor(COLORS[currentShape.getType()]);
                        g.fillRect((currentX + col) * TILE_SIZE, (currentY + row) * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                        g.setColor(Color.BLACK);
                        g.drawRect((currentX + col) * TILE_SIZE, (currentY + row) * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    }
                }
            }
        }

        // Draw the score
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("Score: " + score, 10, 20);

        // Draw game over message
        if (isGameOver) {
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 30));
            g.drawString("Game Over!", BOARD_WIDTH * TILE_SIZE / 2 - 80, BOARD_HEIGHT * TILE_SIZE / 2);
        }

        // Draw pause message
        if (isPaused) {
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 30));
            g.drawString("Paused", BOARD_WIDTH * TILE_SIZE / 2 - 50, BOARD_HEIGHT * TILE_SIZE / 2);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (isGameOver) return;

        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT:
                if (isValidMove(currentShape, currentX - 1, currentY)) currentX--;
                break;
            case KeyEvent.VK_RIGHT:
                if (isValidMove(currentShape, currentX + 1, currentY)) currentX++;
                break;
            case KeyEvent.VK_DOWN:
                if (isValidMove(currentShape, currentX, currentY + 1)) currentY++;
                break;
            case KeyEvent.VK_UP:
                Shape rotated = currentShape.rotate();
                if (isValidMove(rotated, currentX, currentY)) currentShape = rotated;
                break;
            case KeyEvent.VK_SPACE:
                isPaused = !isPaused;
                break;
            case KeyEvent.VK_R:
                startGame();
                break;
        }
        repaint();
    }

    @Override
    public void keyReleased(KeyEvent e) {}
    @Override
    public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        JFrame frame = new JFrame("Tetris");
        Tetris tetris = new Tetris();
        frame.add(tetris);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null); // Center the window
        frame.setVisible(true);
    }
}

class Shape {
    private int[][] shape;
    private int type;

    public Shape() {
        type = (int) (Math.random() * 7);
        shape = SHAPES[type];
    }

    public int getWidth() {
        return shape[0].length;
    }

    public int getHeight() {
        return shape.length;
    }

    public boolean isFilled(int row, int col) {
        return shape[row][col] == 1;
    }

    public int getType() {
        return type;
    }

    public Shape rotate() {
        int[][] rotated = new int[shape[0].length][shape.length];
        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[0].length; col++) {
                rotated[col][shape.length - 1 - row] = shape[row][col];
            }
        }
        Shape newShape = new Shape();
        newShape.shape = rotated;
        newShape.type = this.type;
        return newShape;
    }

    private static final int[][][] SHAPES = {
            {{1, 1, 1, 1}}, // I-shape
            {{1, 1}, {1, 1}}, // O-shape
            {{0, 1, 0}, {1, 1, 1}}, // T-shape
            {{1, 0, 0}, {1, 1, 1}}, // L-shape
            {{0, 0, 1}, {1, 1, 1}}, // J-shape
            {{0, 1, 1}, {1, 1, 0}}, // S-shape
            {{1, 1, 0}, {0, 1, 1}}  // Z-shape
    };
}