package com.seroter.demo.subway;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class SubwayGameLauncher {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Metro Rush");
            SubwayGamePanel panel = new SubwayGamePanel();
            frame.setContentPane(panel);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            panel.startGame();
            panel.requestFocusInWindow();
        });
    }
}

class SubwayGamePanel extends JPanel implements ActionListener {
    private static final int WIDTH = 420;
    private static final int HEIGHT = 760;
    private static final int PLAYER_BASE_Y = HEIGHT - 120;
    private static final int LANE_COUNT = 3;

    private final List<Obstacle> obstacles = new ArrayList<>();
    private final List<Coin> coins = new ArrayList<>();
    private final Random random = new Random();
    private final Timer gameLoop = new Timer(16, this);

    private int currentLane = 1;
    private int score = 0;
    private int highScore = 0;
    private int lives = 3;
    private int speed = 5;
    private double jumpHeight = 0.0;
    private double jumpVelocity = 0.0;
    private double slideTimer = 0.0;
    private double spawnTimer = 0.0;
    private double coinSpawnTimer = 0.0;
    private boolean running = true;
    private boolean gameOver = false;
    private boolean canJump = true;
    private boolean sliding = false;
    private long lastTickMs = System.currentTimeMillis();

    SubwayGamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(10, 18, 30));
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleInput(e);
            }
        });
    }

    public void startGame() {
        resetGame();
        gameLoop.start();
        requestFocusInWindow();
    }

    private void resetGame() {
        obstacles.clear();
        coins.clear();
        currentLane = 1;
        score = 0;
        lives = 3;
        speed = 5;
        jumpHeight = 0;
        jumpVelocity = 0;
        slideTimer = 0;
        spawnTimer = 0;
        coinSpawnTimer = 0;
        running = true;
        gameOver = false;
        canJump = true;
        sliding = false;
        lastTickMs = System.currentTimeMillis();
    }

    private void handleInput(KeyEvent e) {
        if (gameOver) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                resetGame();
            }
            return;
        }

        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT -> currentLane = Math.max(0, currentLane - 1);
            case KeyEvent.VK_RIGHT -> currentLane = Math.min(LANE_COUNT - 1, currentLane + 1);
            case KeyEvent.VK_UP, KeyEvent.VK_SPACE -> {
                if (canJump && !sliding) {
                    jumpVelocity = -17;
                    canJump = false;
                }
            }
            case KeyEvent.VK_DOWN -> {
                if (canJump) {
                    sliding = true;
                    slideTimer = 0.8;
                }
            }
            case KeyEvent.VK_P -> running = !running;
            case KeyEvent.VK_R -> resetGame();
            default -> { }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        drawBackground(g2);
        drawTracks(g2);
        drawCoins(g2);
        drawObstacles(g2);
        drawPlayer(g2);
        drawHud(g2);
        if (gameOver) {
            drawGameOver(g2);
        }
        g2.dispose();
    }

    private void drawBackground(Graphics2D g) {
        GradientPaint gradient = new GradientPaint(0, 0, new Color(9, 17, 31), 0, HEIGHT, new Color(20, 55, 78));
        g.setPaint(gradient);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(new Color(24, 39, 54, 170));
        for (int i = 0; i < 8; i++) {
            int x = i * 60 + 20;
            int y = 100 + (i % 3) * 20;
            g.fillRect(x, y, 34, 110);
            g.fillRect(x + 8, y - 14, 18, 14);
        }
    }

    private void drawTracks(Graphics2D g) {
        int left = 70;
        int laneWidth = (WIDTH - left * 2) / LANE_COUNT;
        int trackTop = 0;
        int trackBottom = HEIGHT;

        g.setColor(new Color(15, 26, 44));
        g.fillRoundRect(left, trackTop, WIDTH - left * 2, trackBottom, 24, 24);

        g.setColor(new Color(67, 84, 102));
        for (int i = 0; i <= LANE_COUNT; i++) {
            int x = left + i * laneWidth;
            g.fillRect(x - 2, 0, 4, HEIGHT);
        }

        g.setColor(new Color(195, 211, 218));
        for (int i = 1; i < LANE_COUNT; i++) {
            int x = left + i * laneWidth;
            g.fillRect(x - 1, 0, 2, HEIGHT);
        }

        g.setColor(new Color(48, 59, 79));
        g.fillRect(left - 16, 0, 16, HEIGHT);
        g.fillRect(WIDTH - left, 0, 16, HEIGHT);

        for (int y = 0; y < HEIGHT; y += 40) {
            g.setColor(new Color(240, 240, 240, 50));
            g.fillRect(left - 10, y, 6, 20);
            g.fillRect(WIDTH - left + 4, y, 6, 20);
        }
    }

    private void drawPlayer(Graphics2D g) {
        int laneCenter = laneCenter(currentLane);
        int x = laneCenter - 18;
        int y = PLAYER_BASE_Y - (int) jumpHeight;
        int bodyWidth = sliding ? 52 : 36;
        int bodyHeight = sliding ? 22 : 48;
        int shadowY = y + bodyHeight + 8;

        g.setColor(new Color(0, 0, 0, 80));
        g.fillOval(x - 6, shadowY, bodyWidth + 12, 12);

        g.setColor(new Color(34, 197, 94));
        g.fillRoundRect(x, y, bodyWidth, bodyHeight, 12, 12);

        g.setColor(new Color(24, 148, 72));
        g.fillRoundRect(x + 10, y + 10, bodyWidth - 20, bodyHeight - 16, 8, 8);

        g.setColor(new Color(245, 208, 73));
        g.fillRoundRect(x + bodyWidth / 2 - 5, y - 14, 10, 12, 8, 8);

        g.setColor(new Color(255, 255, 255));
        g.fillOval(x + 10, y + 8, 6, 6);
        g.fillOval(x + bodyWidth - 16, y + 8, 6, 6);
    }

    private void drawObstacles(Graphics2D g) {
        for (Obstacle obstacle : obstacles) {
            int centerX = laneCenter(obstacle.lane);
            int x = centerX - 18;
            int y = (int) obstacle.y;

            if (obstacle.type == 0) {
                g.setColor(new Color(239, 68, 68));
                g.fillRoundRect(x - 6, y, 48, 56, 12, 12);
                g.setColor(new Color(127, 29, 29));
                g.fillRect(x + 8, y + 18, 8, 20);
                g.fillRect(x + 24, y + 18, 8, 20);
            } else if (obstacle.type == 1) {
                g.setColor(new Color(59, 130, 246));
                g.fillRoundRect(x - 10, y, 56, 62, 10, 10);
                g.setColor(new Color(30, 64, 175));
                g.fillRect(x + 8, y + 10, 12, 18);
                g.fillRect(x + 26, y + 10, 12, 18);
            } else {
                g.setColor(new Color(250, 204, 21));
                g.fillOval(x, y, 42, 42);
                g.setColor(new Color(236, 146, 16));
                g.fillOval(x + 8, y + 8, 26, 26);
            }
        }
    }

    private void drawCoins(Graphics2D g) {
        for (Coin coin : coins) {
            int x = laneCenter(coin.lane) - 12;
            int y = (int) coin.y;
            g.setColor(new Color(250, 204, 21));
            g.fillOval(x, y, 24, 24);
            g.setColor(new Color(255, 240, 145));
            g.fillOval(x + 5, y + 5, 14, 14);
        }
    }

    private void drawHud(Graphics2D g) {
        g.setColor(new Color(11, 22, 39, 190));
        g.fillRoundRect(16, 14, 170, 68, 18, 18);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString("Score: " + score, 28, 40);
        g.drawString("Speed: " + speed, 28, 60);

        g.setColor(new Color(11, 22, 39, 175));
        g.fillRoundRect(WIDTH - 118, 18, 96, 36, 16, 16);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.drawString("Lives: " + lives, WIDTH - 90, 42);

        g.setColor(new Color(11, 22, 39, 175));
        g.fillRoundRect(WIDTH - 118, 60, 96, 36, 16, 16);
        g.setColor(Color.WHITE);
        g.drawString("Best: " + highScore, WIDTH - 100, 84);
    }

    private void drawGameOver(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(new Color(255, 255, 255));
        g.fillRoundRect(70, 250, 280, 180, 20, 20);

        g.setColor(new Color(30, 41, 59));
        g.fillRoundRect(85, 270, 250, 140, 18, 18);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 30));
        g.drawString("GAME OVER", 120, 325);
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.drawString("Score: " + score, 150, 360);
        g.drawString("Press Enter to retry", 120, 392);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!running) {
            repaint();
            return;
        }

        long now = System.currentTimeMillis();
        double delta = now - lastTickMs;
        lastTickMs = now;

        updatePlayer(delta);
        updateSpawns(delta);
        updateWorld(delta);
        checkCollisions();

        if (score > highScore) {
            highScore = score;
        }

        repaint();
    }

    private void updatePlayer(double delta) {
        if (jumpVelocity != 0 || jumpHeight > 0) {
            jumpHeight += jumpVelocity * (delta / 16.0);
            jumpVelocity += 0.9 * (delta / 16.0);
            if (jumpHeight >= 0) {
                jumpHeight = 0;
                jumpVelocity = 0;
                canJump = true;
            }
        }

        if (sliding) {
            slideTimer -= delta / 1000.0;
            if (slideTimer <= 0) {
                sliding = false;
            }
        }
    }

    private void updateSpawns(double delta) {
        spawnTimer += delta;
        coinSpawnTimer += delta;

        double obstacleDelay = Math.max(500, 1200 - speed * 28);
        if (spawnTimer >= obstacleDelay) {
            spawnTimer = 0;
            spawnObstacle();
            speed = Math.min(18, speed + 1);
        }

        if (coinSpawnTimer >= 1000) {
            coinSpawnTimer = 0;
            spawnCoin();
        }
    }

    private void updateWorld(double delta) {
        for (Iterator<Obstacle> iterator = obstacles.iterator(); iterator.hasNext(); ) {
            Obstacle obstacle = iterator.next();
            obstacle.y += (speed * delta / 16.0);
            if (obstacle.y > HEIGHT + 100) {
                iterator.remove();
            }
        }

        for (Iterator<Coin> iterator = coins.iterator(); iterator.hasNext(); ) {
            Coin coin = iterator.next();
            coin.y += ((speed + 2) * delta / 16.0);
            if (coin.y > HEIGHT + 50) {
                iterator.remove();
            }
        }
    }

    private void checkCollisions() {
        Rectangle playerBounds = playerBounds();

        for (Iterator<Obstacle> iterator = obstacles.iterator(); iterator.hasNext(); ) {
            Obstacle obstacle = iterator.next();
            Rectangle obstacleBounds = obstacleBounds(obstacle);
            if (playerBounds.intersects(obstacleBounds)) {
                iterator.remove();
                damagePlayer();
                break;
            }
        }

        for (Iterator<Coin> iterator = coins.iterator(); iterator.hasNext(); ) {
            Coin coin = iterator.next();
            Rectangle coinBounds = new Rectangle(laneCenter(coin.lane) - 10, (int) coin.y, 20, 20);
            if (playerBounds.intersects(coinBounds)) {
                iterator.remove();
                score += 10;
            }
        }
    }

    private Rectangle playerBounds() {
        int x = laneCenter(currentLane) - 18;
        int y = PLAYER_BASE_Y - (int) jumpHeight;
        int width = sliding ? 52 : 36;
        int height = sliding ? 22 : 48;
        return new Rectangle(x, y, width, height);
    }

    private Rectangle obstacleBounds(Obstacle obstacle) {
        int centerX = laneCenter(obstacle.lane);
        int x = centerX - 18;
        int y = (int) obstacle.y;

        int width;
        int height;
        if (obstacle.type == 0) {
            width = 48;
            height = 56;
        } else if (obstacle.type == 1) {
            width = 56;
            height = 62;
        } else {
            width = 42;
            height = 42;
        }

        return new Rectangle(x, y, width, height);
    }

    private void damagePlayer() {
        lives--;
        if (lives <= 0) {
            gameOver = true;
            running = false;
        }
    }

    private void spawnObstacle() {
        int lane = random.nextInt(LANE_COUNT);
        int type = random.nextInt(3);
        obstacles.add(new Obstacle(lane, -80, type));
    }

    private void spawnCoin() {
        int lane = random.nextInt(LANE_COUNT);
        coins.add(new Coin(lane, -30));
    }

    private int laneCenter(int lane) {
        int left = 70;
        int laneWidth = (WIDTH - left * 2) / LANE_COUNT;
        return left + lane * laneWidth + laneWidth / 2;
    }

    private static class Obstacle {
        private final int lane;
        private double y;
        private final int type;

        private Obstacle(int lane, double y, int type) {
            this.lane = lane;
            this.y = y;
            this.type = type;
        }
    }

    private static class Coin {
        private final int lane;
        private double y;

        private Coin(int lane, double y) {
            this.lane = lane;
            this.y = y;
        }
    }
}
