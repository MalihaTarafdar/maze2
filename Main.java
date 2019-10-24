import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class Main extends JPanel implements KeyListener, Runnable {
	private static final long serialVersionUID = 1L;

	private JFrame frame;
	private Thread thread;
	
	private Structure[][] maze;
	private GameState gameState = GameState.MAIN_MENU;
	private Explorer explorer;
	private Location end;
	private Maze map;
	
	private boolean onMaze2D;
	private boolean paused;
	private boolean win;
	
	private Menu mainMenu = new Menu("2D Maze", "3D Maze", "Settings", "Quit");
	private Menu levelSelectMenu = new Menu("Level 1", "Level 2", "Level 3");
	private Menu pauseMenu = new Menu("Resume", "Quit");

	private Font title = new Font("Positive System", Font.PLAIN, 100);
	private Font main = new Font("Game Over", Font.PLAIN, 70);
	private Font other = new Font("Game Over", Font.PLAIN, 100);
	private FontMetrics tm;
	private FontMetrics mm;
	private FontMetrics om;

	private final int renderDistance = 5;

	enum GameState {
		MAIN_MENU, LEVEL_SELECT, SETTINGS, MAZE2D, MAZE3D, GAME_OVER;
	}

	public Main() {		
		try {
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("./fonts/Positive System.otf")));
			ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("./fonts/game_over.ttf")));
		} catch (IOException | FontFormatException e) {}

		frame = new JFrame("Maze");
		frame.add(this);
		frame.addKeyListener(this);
		frame.setSize(1000, 800);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(false);
		frame.setVisible(true);

		thread = new Thread(this);
		thread.start();
	}

	public void run() {
		while (true) {
			switch (gameState) {
				case MAZE2D:
				case MAZE3D:
					if (explorer.getHealth() == 0) {
						win = false;
						gameState = GameState.GAME_OVER;
					}
					if (explorer.getLoc().getRow() == end.getRow() && explorer.getLoc().getCol() == end.getCol()) {
						win = true;
						gameState = GameState.GAME_OVER;
					}
					break;
				case GAME_OVER:
					delay(1000);
					gameState = GameState.MAIN_MENU;
					reset();
					repaint();
					break;
				case MAIN_MENU:
					break;
				case LEVEL_SELECT:
					break;
				case SETTINGS:
					break;
			}
			repaint();
		}
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D)g;
		g2.setColor(Color.BLACK);
		g2.fillRect(0, 0, frame.getWidth(), frame.getHeight());

		tm = g2.getFontMetrics(title);
		mm = g2.getFontMetrics(main);
		om = g2.getFontMetrics(other);

		switch (gameState) {
			case MAIN_MENU: paintMenu(g2);
				break;
			case LEVEL_SELECT: paintLevelSelect(g2);
				break;
			case MAZE2D: paintMaze2D(g2);
				if (paused) {
					paintPauseMenu(g2);
				}
				break;
			case MAZE3D: paintMaze3D(g2);
				if (paused) {
					paintPauseMenu(g2);
				}
				break;
			case SETTINGS: paintSettings(g2);
				break;
			case GAME_OVER: paintGameOver(g2);
				break;
		}
	}

	public void paintMaze3D(Graphics2D g2) {
		ArrayList<Wall3D> walls = getWalls();
		for (int i = 0; i < walls.size(); i++) {
			g2.setColor(walls.get(i).getColor());
			g2.fill(walls.get(i).getPolygon());
		}
	}

	public ArrayList<Wall3D> getWalls() {
		int currentRow = explorer.getLoc().getRow(), currentCol = explorer.getLoc().getCol();
		ArrayList<Wall3D> walls = new ArrayList<Wall3D>();
		ArrayList<Wall3D> temp = new ArrayList<Wall3D>();

		for (int d = 0; d < renderDistance; d++) {
			int[] xpoints = {50 + 50 * d, 100 + 50 * d, 100 + 50 * d, 50 + 50 * d};
			int[] ypoints = {50 + 50 * d, 100 + 50 * d, 700 - 50 * d, 750 - 50 * d};
			Color color = new Color(128 - 25 * d, 128 - 25 * d, 128 - 25 * d);

			switch(explorer.getDir()) {
				case EAST:
					if (maze[currentRow - 1][currentCol + d] != null) {
						walls.add(new Wall3D(new Polygon(xpoints, ypoints, 4), color));
					}
					if (maze[currentRow + 1][currentCol + d] != null) {
						walls.add(new Wall3D(reflectPolygon(new Polygon(xpoints, ypoints, 4), false), color));
					}
					break;
				case NORTH:
					break;
				case WEST:
					break;
				case SOUTH:
					break;
			}
			if (d < 4) {
				int width = 50;
				int height = ypoints[xpoints.length - 1 - d] - ypoints[d];
				Polygon p = convertRect(new Rectangle(xpoints[d] - width, ypoints[d], width, height));
				temp.add(new Wall3D(p, new Color(color.getRed() - 25, color.getGreen() - 25, color.getBlue() - 25)));
				temp.add(new Wall3D(reflectPolygon(p, false), new Color(color.getRed() - 25, color.getGreen() - 25, color.getBlue() - 25)));
			}
		}

		for (Wall3D w : temp) {
			walls.add(0, w);
		}

		return walls;
	}

	public Polygon convertRect(Rectangle r) {
		int[] xpoints = {(int)r.getX(), (int)(r.getX() + r.getWidth()), (int)(r.getX() + r.getWidth()), (int)(r.getX())};
		int[] ypoints = {(int)r.getY(), (int)(r.getY()), (int)(r.getY() + r.getHeight()), (int)(r.getY() + r.getHeight())};
		return new Polygon(xpoints, ypoints, 4);
	}
	public Polygon reflectPolygon(Polygon p, boolean horizontal) {
		Polygon reflectedP = new Polygon();
		for (int i = 0; i < p.npoints; i++) {
			if (horizontal) {
				reflectedP.addPoint(p.xpoints[i], frame.getHeight() - p.ypoints[i]);
			} else {
				reflectedP.addPoint(frame.getWidth() - p.xpoints[i], p.ypoints[i]);
			}
		}
		return reflectedP;
	}

	public static class Wall3D {
		private final Polygon polygon;
		private final Color color;
		public Wall3D(Polygon polygon, Color color) {
			this.polygon = polygon;
			this.color = color;
		}
		public Polygon getPolygon() {
			return polygon;
		}
		public Color getColor() {
			return color;
		}
	}

	public void paintMaze2D(Graphics2D g2) {
		for (Structure[] row : maze) {
			for (Structure s : row) {
				if (s != null) {
					g2.setColor(s.getColor());
					g2.fillRect(s.getLoc().getCol() * s.getSize(), s.getLoc().getRow() * s.getSize(), s.getSize(), s.getSize());
				}
			}
		}
		g2.setColor(explorer.getColor());
		g2.fillOval(explorer.getLoc().getCol() * explorer.getSize(), explorer.getLoc().getRow() * explorer.getSize(), explorer.getSize(), explorer.getSize());
		g2.drawString(explorer.getDir() + "", 800, 50);
	}

	public void paintPauseMenu(Graphics2D g2) {
		g2.setColor(Color.WHITE);
		g2.fillRect(350, 100, 200, 200);
		g2.setColor(Color.BLACK);
		g2.fillRect(360, 110, 180, 180);

		g2.setFont(main);
		g2.setColor(Color.WHITE);
		int optionY = 180;
		for (Menu.Option option : pauseMenu.getOptions()) {
			g2.drawString(option.getName(), 420, optionY);
			optionY += 50;
		}
		for (int i = 0; i < pauseMenu.getOptions().length; i++) {
			if (pauseMenu.getOptions()[i].isSelected()) {
				optionY = 165 + 50 * i;
				g2.setColor(Color.BLUE);
				g2.fillRect(390, optionY, 10, 10);
			}
		}
	}

	public void paintSettings(Graphics2D g2) {

	}

	public void paintGameOver(Graphics2D g2) {
		g2.setColor(Color.WHITE);
		g2.setFont(other);
		if (win) {
			g2.drawString("YOU WIN", frame.getWidth() / 2 - om.stringWidth("YOU WIN") / 2, frame.getHeight() / 2);
		} else {
			g2.drawString("YOU LOSE", frame.getWidth() / 2 - om.stringWidth("YOU LOSE") / 2, frame.getHeight() / 2);
		}
	}

	public void paintLevelSelect(Graphics2D g2) {
		int titleX = frame.getWidth() / 2 - om.stringWidth("Select Level") / 2;
		int titleY = frame.getHeight() / 4;
		g2.setFont(other);
		g2.setPaint(new GradientPaint(titleX, titleY, Color.BLUE, titleX + om.stringWidth("Select Level"), titleY + om.getHeight(), Color.CYAN));
		g2.drawString("Select Level", titleX, titleY);

		g2.setColor(Color.WHITE);
		g2.setFont(main);
		int optionX = frame.getWidth() / 2 - 250;
		for (Menu.Option option : levelSelectMenu.getOptions()) {
			g2.drawString(option.getName(), optionX, frame.getHeight() * 2 / 3);
			optionX += 200;
		}
		optionX = frame.getWidth() / 2 - 250 - om.stringWidth("Level 3") / 2;
		for (int i = 0; i < levelSelectMenu.getOptions().length; i++) {
			if (levelSelectMenu.getOptions()[i].isSelected()) {
				optionX += 200 * i;
				g2.setColor(Color.BLUE);
				g2.drawRect(optionX, frame.getHeight() / 2 - 50, 200, 200);
			}
		}
	}

	public void paintMenu(Graphics2D g2) {
		int titleX = frame.getWidth() / 2 - tm.stringWidth("MAZE") / 2;
		int titleY = frame.getHeight() / 4;
		g2.setFont(title);
		g2.setPaint(new GradientPaint(titleX, titleY, Color.BLUE, titleX + tm.stringWidth("MAZE"), titleY + tm.getHeight(), Color.CYAN));
		g2.drawString("MAZE", titleX, titleY);

		g2.setColor(Color.WHITE);
		g2.setFont(other);
		int optionY = frame.getHeight() / 2;

		for (Menu.Option option : mainMenu.getOptions()) {
			g2.drawString(option.getName(), frame.getWidth() / 2 - mm.stringWidth(option.getName()) / 2, optionY);
			optionY += 55;
		}

		for (int i = 0; i < mainMenu.getOptions().length; i++) {
			if (mainMenu.getOptions()[i].isSelected()) {
				optionY = frame.getHeight() / 2 + 57 * i - 20;
				g2.setColor(Color.BLUE);
				g2.fillRect(frame.getWidth() / 2 - 140, optionY, 10, 10);
			}
		}
	}

	public void keyPressed(KeyEvent e) {
		if (paused) {
			if (e.getKeyCode() == KeyEvent.VK_UP) {
				pauseMenu.moveBackward();
			} else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
				pauseMenu.moveForward();
			} else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				if (pauseMenu.getOptions()[1].isSelected()) {
					gameState = GameState.MAIN_MENU;
				}
				paused = false;
			}
		} else {
			switch (gameState) {
				case MAZE3D:
				case MAZE2D:
					if (e.getKeyCode() == KeyEvent.VK_UP) {
						explorer.move(maze);
					} else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
						explorer.turn(Explorer.RelativeDirection.LEFT);
					} else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
						explorer.turn(Explorer.RelativeDirection.RIGHT);
					} else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
						paused = true;
					}
					break;
				case MAIN_MENU:
					if (e.getKeyCode() == KeyEvent.VK_UP) {
						mainMenu.moveBackward();
					} else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
						mainMenu.moveForward();
					} else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
						if (mainMenu.getOptions()[0].isSelected()) {
							onMaze2D = true;
							gameState = GameState.LEVEL_SELECT;
						} else if (mainMenu.getOptions()[1].isSelected()) {
							onMaze2D = false;
							gameState = GameState.LEVEL_SELECT;
						} else if (mainMenu.getOptions()[2].isSelected()) {
							gameState = GameState.SETTINGS;
						} else {
							System.exit(0);
						}
					}
					break;
				case LEVEL_SELECT:
					if (e.getKeyCode() == KeyEvent.VK_LEFT) {
						levelSelectMenu.moveBackward();
					} else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
						levelSelectMenu.moveForward();
					} else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
						if (levelSelectMenu.getOptions()[0].isSelected()) {
							map = new Maze(new File("./mazes/maze1.txt"));
						} else if (levelSelectMenu.getOptions()[1].isSelected()) {
							map = new Maze(new File("./mazes/maze2.txt"));
						} else {
							map = new Maze(new File("./mazes/maze3.txt"));
						}
						maze = map.getMaze();
						explorer = map.getExplorer();
						end = map.getEnd();
						if (onMaze2D) {
							gameState = GameState.MAZE2D;
						} else {
							gameState = GameState.MAZE3D;
						}
					} else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
						gameState = GameState.MAIN_MENU;
					}
					break;
				case SETTINGS:
					if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
						gameState = GameState.MAIN_MENU;
					}
					break;
				case GAME_OVER:
					break;
			}
		}

		if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_ESCAPE) {
			mainMenu.resetOptions();
			levelSelectMenu.resetOptions();
			pauseMenu.resetOptions();
		}
		repaint();
	}
	public void keyReleased(KeyEvent e) {}
	public void keyTyped(KeyEvent e) {}

	public void reset() {
		win = false;
		for (int i = 0; i < maze.length; i++) {
			for (int j = 0; j < maze[0].length; j++) {
				maze[i][j] = null;
			}
		}
	}

	public void delay(int ms) {
		try {
			Thread.sleep(ms);
		} catch(InterruptedException e){}
	}

	public static void main(String[] args) {
		Main game = new Main();
	}
}