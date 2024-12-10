import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
class Multiplayer {

    static class Gamemenu {
        JFrame menuFrame = new JFrame("TicTacToe ---AA");

        Gamemenu() {
            menuFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            menuFrame.setSize(400, 400);
            menuFrame.setLayout(new GridLayout(5, 1));
            menuFrame.setLocationRelativeTo(null);

            JLabel title = new JLabel("Tic-Tac-Toe", SwingConstants.CENTER);
            title.setFont(new Font("Arial", Font.BOLD, 30));
            menuFrame.add(title);

            JButton playButton = new JButton("Play with Friend");
            playButton.setFont(new Font("Arial", Font.BOLD, 20));
            playButton.addActionListener(e -> {
                menuFrame.dispose();
                new Gamemenu2();
            });
            menuFrame.add(playButton);

            JButton planButton = new JButton("Game Plan");
            planButton.setFont(new Font("Arial", Font.BOLD, 20));
            planButton.addActionListener(e -> {
                JOptionPane.showMessageDialog(menuFrame, "Form a line of three symbols in a row, column, or diagonal to win.");
            });
            menuFrame.add(planButton);

            JButton quitButton = new JButton("Quit");
            quitButton.setFont(new Font("Arial", Font.BOLD, 20));
            quitButton.addActionListener(e -> System.exit(0));
            menuFrame.add(quitButton);

            menuFrame.setVisible(true);
        }
    }

    static class Gamemenu2 {
        Gamemenu2() {
            JFrame frame = new JFrame("TicTacToe");
            frame.setSize(400, 300);
            frame.setLayout(new GridLayout(3, 1));
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JButton hostButton = new JButton("Host Game");
            JButton joinButton = new JButton("Join Game");
            JButton quitButton = new JButton("Quit");

            hostButton.addActionListener(e -> {
                frame.dispose();
                new Host();
            });

            joinButton.addActionListener(e -> {
                frame.dispose();
                new Join();
            });

            quitButton.addActionListener(e -> System.exit(0));

            frame.add(hostButton);
            frame.add(joinButton);
            frame.add(quitButton);

            frame.setVisible(true);
        }
    }

    static class Host {
        private ServerSocket serverSocket;
        private Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;

        public Host() {
            JFrame frame = new JFrame("Host Game");
            frame.setSize(400, 300);
            frame.setLayout(new FlowLayout());
            frame.setLocationRelativeTo(null);

            JLabel label = new JLabel("Waiting for a player to join... IP: " + ip());
            frame.add(label);

            new Thread(() -> {
                try {
                    serverSocket = new ServerSocket(12345); // Host on port 12345
                    socket = serverSocket.accept(); // Wait for a client to join
                    out = new ObjectOutputStream(socket.getOutputStream());
                    in = new ObjectInputStream(socket.getInputStream());

                    SwingUtilities.invokeLater(() -> {
                        frame.dispose();
                        new TicTacToe(false, out, in); // Start game as Player X
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setVisible(true);
        }
    }

    static class Join {
        private Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;

        public Join() {
            JFrame frame = new JFrame("Join Game");
            frame.setSize(400, 300);
            frame.setLayout(new FlowLayout());
            frame.setLocationRelativeTo(null);

            JLabel label = new JLabel("Enter Host IP Address:");
            JTextField ipField = new JTextField(15);
            JButton connectButton = new JButton("Connect");

            frame.add(label);
            frame.add(ipField);
            frame.add(connectButton);

            connectButton.addActionListener(e -> {
                String ipAddress = ipField.getText().trim();
                try {
                    socket = new Socket(ipAddress, 12345); // Connect to host
                    out = new ObjectOutputStream(socket.getOutputStream());
                    in = new ObjectInputStream(socket.getInputStream());

                    frame.dispose();
                    new TicTacToe(true, out, in); // Start game as Player O
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(frame, "Failed to connect to host. Try again.");
                    ex.printStackTrace();
                }
            });

            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setVisible(true);
        }
    }

    static class TicTacToe {
        JFrame frame = new JFrame("Tic-Tac-Toe");
        JLabel text = new JLabel();
        JPanel boardPanel = new JPanel();
        JButton[][] board = new JButton[3][3];
        boolean isPlayerO;
        boolean myTurn;
        boolean gameover = false;
        int turns = 0;

        ObjectOutputStream out;
        ObjectInputStream in;

        // To store the history of moves for undo/redo functionality
        Stack<Move> moveHistory = new Stack<>();
        Stack<Move> redoHistory = new Stack<>();

        public TicTacToe(boolean isPlayerO, ObjectOutputStream out, ObjectInputStream in) {
            this.isPlayerO = isPlayerO;
            this.out = out;
            this.in = in;
            this.myTurn = !isPlayerO; // Player X always starts

            initializeGame();
            startListenerThread();
        }

        private void initializeGame() {
            frame.setSize(600, 700);
            frame.setLocationRelativeTo(null);
            frame.setLayout(new BorderLayout());
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            text.setText("Tic-Tac-Toe | Waiting for Opponent");
            text.setHorizontalAlignment(JLabel.CENTER);
            text.setOpaque(true);
            text.setFont(new Font("Arial", Font.BOLD, 30));
            frame.add(text, BorderLayout.NORTH);

            boardPanel.setLayout(new GridLayout(3, 3));
            frame.add(boardPanel, BorderLayout.CENTER);

            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    JButton tile = new JButton();
                    board[r][c] = tile;
                    boardPanel.add(tile);
                    tile.setFont(new Font("Arial", Font.BOLD, 120));
                    tile.setFocusable(false);
                    final int row = r, col = c;

                    tile.addActionListener(e -> {
                        if (myTurn && !gameover && tile.getText().equals("")) {
                            makeMove(row, col, "MOVE");
                        }
                    });
                }
            }

            JButton menuButton = new JButton("Back to Menu");
            menuButton.addActionListener(e -> {
                frame.dispose();
                new Gamemenu();
            });
            frame.add(menuButton, BorderLayout.SOUTH);

            frame.setVisible(true);
        }

        private void startListenerThread() {
            new Thread(() -> {
                try {
                    while (!gameover) {
                        Move move = (Move) in.readObject();
                        if (move.action.equals("MOVE")) {
                            updateBoard(move.row, move.col, move.player);
                        } else if (move.action.equals("WIN")) {
                            gameover = true;
                            text.setText(move.player + " Wins!");
                        } else if (move.action.equals("DRAW")) {
                            gameover = true;
                            text.setText("It's a Draw!");
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }).start();
        }

        private void makeMove(int row, int col, String action) {
            try {
                String player = isPlayerO ? "O" : "X";
                Move move = new Move(row, col, action, player);
                out.writeObject(move);
                out.flush();

                if (action.equals("MOVE")) {
                    updateBoard(row, col, player);
                    moveHistory.push(move); // Store move in history for undo/redo
                    redoHistory.clear(); // Clear redo stack after new move
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void updateBoard(int row, int col, String player) {
            if (row >= 0 && col >= 0) {
                board[row][col].setText(player);
                turns++;
                checkGameStatus(player);
            }
            myTurn = !myTurn;
            text.setText(myTurn ? "Your Turn" : "Opponent's Turn");
        }

        private void checkGameStatus(String player) {
            String[][] grid = new String[3][3];
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    grid[r][c] = board[r][c].getText();
                }
            }

            for (int i = 0; i < 3; i++) {
                if (grid[i][0].equals(player) && grid[i][1].equals(player) && grid[i][2].equals(player)) {
                    sendGameOver(player);
                    return;
                }
                if (grid[0][i].equals(player) && grid[1][i].equals(player) && grid[2][i].equals(player)) {
                    sendGameOver(player);
                    return;
                }
            }

            if (grid[0][0].equals(player) && grid[1][1].equals(player) && grid[2][2].equals(player)) {
                sendGameOver(player);
                return;
            }
            if (grid[0][2].equals(player) && grid[1][1].equals(player) && grid[2][0].equals(player)) {
                sendGameOver(player);
                return;
            }

            if (turns == 9) {
                sendGameOver("DRAW");
            }
        }

        private void sendGameOver(String result) {
            try {
                String action = result.equals("DRAW") ? "DRAW" : "WIN";
                out.writeObject(new Move(-1, -1, action, result));
                out.flush();
                gameover = true;
                text.setText(result.equals("DRAW") ? "It's a Draw!" : result + " Wins!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Undo move
        private void undoMove() {
            if (!moveHistory.isEmpty()) {
                Move lastMove = moveHistory.pop();
                redoHistory.push(lastMove);
                board[lastMove.row][lastMove.col].setText("");
                turns--;
                myTurn = !myTurn;
                text.setText(myTurn ? "Your Turn" : "Opponent's Turn");
            }
        }

        // Redo move
        private void redoMove() {
            if (!redoHistory.isEmpty()) {
                Move lastRedo = redoHistory.pop();
                updateBoard(lastRedo.row, lastRedo.col, lastRedo.player);
            }
        }
    }

    static class Move implements Serializable {
        int row, col;
        String action, player;

        public Move(int row, int col, String action, String player) {
            this.row = row;
            this.col = col;
            this.action = action;
            this.player = player;
        }
    }

    public static String ip() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    if (inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
            return "No active IPv4 address found.";
        } catch (SocketException e) {
            return "Error retrieving network interfaces: " + e.getMessage();
        }
    }
}

public class TicTacToeGame extends Multiplayer.Gamemenu2{
    static Multiplayer mul=new Multiplayer();
    
        public static void main(String[] args) {
            new Gamemenu(); // Show the game menu and starting point*
        }
    
        static class Gamemenu {
            JFrame menuFrame = new JFrame("TicTacToe ---AA");
    
            Gamemenu() {
                menuFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                menuFrame.setSize(400, 400);
                menuFrame.setLayout(new GridLayout(6, 1));
                menuFrame.setLocationRelativeTo(null);
    
                JLabel title = new JLabel("Tic-Tac-Toe", SwingConstants.CENTER);
                title.setFont(new Font("Arial", Font.BOLD, 30));
                menuFrame.add(title);
    
                JButton playButton = new JButton("Play");
                playButton.setFont(new Font("Arial", Font.BOLD, 20));
                playButton.addActionListener(e -> {
                    menuFrame.dispose();
                    new TicTacToe(false); // Start two-player game
                });
                menuFrame.add(playButton);
    
                JButton playWithComputerButton = new JButton("Play with Computer");
                playWithComputerButton.setFont(new Font("Arial", Font.BOLD, 20));
                playWithComputerButton.addActionListener(e -> {
                    menuFrame.dispose();
                    new TicTacToe(true); // Start game against computer
                });
                menuFrame.add(playWithComputerButton);
    
                JButton onlineButton = new JButton("Online");
                onlineButton.setFont(new Font("Arial", Font.BOLD, 20));
                onlineButton.addActionListener(e -> {
                    menuFrame.dispose();
                    new Multiplayer.Gamemenu2();//headache statement...../
                    
                
            });
            menuFrame.add(onlineButton);

            JButton planButton = new JButton("Plan");
            planButton.setFont(new Font("Arial", Font.BOLD, 20));
            planButton.addActionListener(e -> {
                JOptionPane.showMessageDialog(menuFrame, "Game plan: Try to win by forming a line of three symbols in a row, column, or diagonal.");
            });
            menuFrame.add(planButton);

            JButton quitButton = new JButton("Quit");
            quitButton.setFont(new Font("Arial", Font.BOLD, 20));
            quitButton.addActionListener(e -> System.exit(0));
            menuFrame.add(quitButton);

            menuFrame.setVisible(true);
        }
    }

    static class TicTacToe {

        JFrame frame = new JFrame("TicTacToe");
        JLabel text = new JLabel();
        JPanel boardPanel = new JPanel();
        JButton[][] board = new JButton[3][3];
        boolean againstComputer;
        boolean playerTurn = true;
        boolean gameover = false;
        int turns = 0;

        // For Undo and Redo
        Stack<Move> moveHistory = new Stack<>();
        Stack<Move> redoStack = new Stack<>();

        // Scoreboard
        int playerXScore = 0;
        int playerOScore = 0;
        int tieScore = 0;

        TicTacToe(boolean againstComputer) {
            this.againstComputer = againstComputer;

            frame.setVisible(true);
            frame.setSize(600, 700);
            frame.setLocationRelativeTo(null);
            frame.setLayout(new BorderLayout());
            frame.setResizable(false);

            // Scoreboard Label
            text.setText("Tic-Tac-Toe | Player X's Turn");
            text.setBackground(Color.BLACK);
            text.setForeground(Color.WHITE);
            text.setHorizontalAlignment(JLabel.CENTER);
            text.setOpaque(true);
            text.setFont(new Font("Arial", Font.BOLD, 30));
            frame.add(text, BorderLayout.NORTH);

            // Board Panel
            boardPanel.setLayout(new GridLayout(3, 3));
            frame.add(boardPanel, BorderLayout.CENTER);

            // Initialize Board
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    JButton tile = new JButton();
                    board[r][c] = tile;
                    boardPanel.add(tile);
                    tile.setFont(new Font("Arial", Font.BOLD, 120));
                    tile.setFocusable(false);
                    final int row = r, col = c;
                    tile.addActionListener(e -> handleMove(tile, row, col));
                }
            }

            // Bottom Panel for Buttons
            JPanel bottomPanel = new JPanel();
            bottomPanel.setLayout(new GridLayout(1, 4));

            JButton undoButton = new JButton("Undo");
            undoButton.addActionListener(e -> undoMove());
            bottomPanel.add(undoButton);

            JButton redoButton = new JButton("Redo");
            redoButton.addActionListener(e -> redoMove());
            bottomPanel.add(redoButton);

            JButton resetButton = new JButton("Reset");
            resetButton.addActionListener(e -> resetGame());
            bottomPanel.add(resetButton);

            JButton menuButton = new JButton("Menu");
            menuButton.addActionListener(e -> {
                frame.dispose();
                new Gamemenu();
            });
            bottomPanel.add(menuButton);

            frame.add(bottomPanel, BorderLayout.SOUTH);
        }

        void handleMove(JButton tile, int row, int col) {
            if (gameover || !tile.getText().isEmpty() || (againstComputer && !playerTurn)) return;

            String currentPlayer = playerTurn ? "X" : "O";
            tile.setText(currentPlayer);
            turns++;

            moveHistory.push(new Move(row, col, currentPlayer));
            redoStack.clear(); // Clear redo stack on a new move

            if (checkWinner(currentPlayer)) {
                updateScore(currentPlayer);
                text.setText("Player " + currentPlayer + " Wins!");
                gameover = true;
                return;
            }

            if (turns == 9) {
                tieScore++;
                text.setText("It's a Tie!");
                gameover = true;
                return;
            }

            playerTurn = !playerTurn;
            text.setText("Player " + (playerTurn ? "X" : "O") + "'s Turn");

            if (againstComputer && !playerTurn && !gameover) {
                computerMove();
            }
        }

        void computerMove() {
            // The magic square position map, correlating each position number with its (row, col)
            int[][] magicSquare = {
                {8, 1, 6},
                {3, 5, 7},
                {4, 9, 2}
            };

            // Priority list for computer moves, from high to low based on the Magic Square strategy
            int[] moveOrder = {5, 1, 3, 7, 9, 4, 8, 6, 2}; // Center, corners, then sides in order

            // Find the first available move in the Magic Square order
            for (int move : moveOrder) {
                int row = (move - 1) / 3;
                int col = (move - 1) % 3;

                // If the spot is empty, make the move
                if (board[row][col].getText().isEmpty()) {
                    board[row][col].setText("O");
                    turns++;
                    moveHistory.push(new Move(row, col, "O"));

                    // Check if computer wins
                    if (checkWinner("O")) {
                        updateScore("O");
                        text.setText("Computer Wins!");
                        gameover = true;
                    } else if (turns == 9) {
                        tieScore++;
                        text.setText("It's a Tie!");
                        gameover = true;
                    } else {
                        playerTurn = true;
                        text.setText("Player X's Turn");
                    }
                    return;
                }
            }
        }

        void undoMove() {
            if (moveHistory.isEmpty() || gameover) return;

            Move lastMove = moveHistory.pop();
            redoStack.push(lastMove);
            board[lastMove.row][lastMove.col].setText("");
            turns--;

            if (!againstComputer || lastMove.symbol.equals("X")) {
                playerTurn = lastMove.symbol.equals("O");
                text.setText("Player " + (playerTurn ? "X" : "O") + "'s Turn");
            } else if (!moveHistory.isEmpty()) {
                // Undo computer's last move too
                Move computerMove = moveHistory.pop();
                redoStack.push(computerMove);
                board[computerMove.row][computerMove.col].setText("");
                turns--;
            }

            gameover = false;
        }

        void redoMove() {
            if (redoStack.isEmpty() || gameover) return;

            Move move = redoStack.pop();
            board[move.row][move.col].setText(move.symbol);
            moveHistory.push(move);
            turns++;

            if (checkWinner(move.symbol)) {
                updateScore(move.symbol);
                text.setText("Player " + move.symbol + " Wins!");
                gameover = true;
                return;
            }

            if (turns == 9) {
                tieScore++;
                text.setText("It's a Tie!");
                gameover = true;
                return;
            }

            playerTurn = move.symbol.equals("O");
            text.setText("Player " + (playerTurn ? "X" : "O") + "'s Turn");
        }

        void resetGame() {
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    board[r][c].setText("");
                }
            }
            moveHistory.clear();
            redoStack.clear();
            turns = 0;
            gameover = false;
            playerTurn = true;
            text.setText("Player X's Turn");
        }

        boolean checkWinner(String symbol) {
            // Check rows, columns, and diagonals for a winner
            for (int i = 0; i < 3; i++) {
                if (board[i][0].getText().equals(symbol) && board[i][1].getText().equals(symbol) && board[i][2].getText().equals(symbol)) {
                    return true;
                }
                if (board[0][i].getText().equals(symbol) && board[1][i].getText().equals(symbol) && board[2][i].getText().equals(symbol)) {
                    return true;
                }
            }
            if (board[0][0].getText().equals(symbol) && board[1][1].getText().equals(symbol) && board[2][2].getText().equals(symbol)) {
                return true;
            }
            if (board[0][2].getText().equals(symbol) && board[1][1].getText().equals(symbol) && board[2][0].getText().equals(symbol)) {
                return true;
            }
            return false;
        }

        void updateScore(String winner) {
            if (winner.equals("X")) {
                playerXScore++;
            } else if (winner.equals("O")) {
                playerOScore++;
            }
            text.setText("Player X: " + playerXScore + " | Player O: " + playerOScore + " | Ties: " + tieScore);
        }

    }

    static class Move {
        int row, col;
        String symbol;

        Move(int row, int col, String symbol) {
            this.row = row;
            this.col = col;
            this.symbol = symbol;
        }
    }
}