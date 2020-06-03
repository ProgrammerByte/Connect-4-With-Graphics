package com.mygdx.game;

import java.util.Random;
import java.util.Scanner;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

public class Connect4Graphics extends ApplicationAdapter {
	SpriteBatch batch;
	BitmapFont font;
	ShapeRenderer sr;
	static int xRes = 1440, yRes = 1080;
	static float[][][] centres; //of holes
	static int[][] board;
	static int currentTurn = -1, difficulty = 1, first; //min = 1, max = 4
	static boolean isHeld = false, AI = false;
	static String output = "";
	static String[] players = {"Red", "Yellow"};
	
	public static int calculateFitness(int[][] board) {
		int fitness = 0;
		for (int i = 0; i < 6; i++) {
			if (board[i][3] == 2) { //if piece is in the centre
				fitness += 3;
			}
		}
		//max coord = 5, 6
		int[][] combinations = new int[][] {{1, 0, 0, 0, 2, 6},
											{0, 1, 0, 0, 5, 3}, 
											{1, 1, 0, 0, 2, 3}, 
											{1, -1, 0, 4, 2, 6}}; //iDir, xDir, iMin, xMin, iMax, xMax
											
		int iVal, xVal; //current coordinates
		int xCount, oCount; //Counts player tiles in a line
		
		for (int a = 0; a < 4; a++) { //every combination
			for (int i = combinations[a][2]; i <= combinations[a][4]; i++) { //for every valid start position for a line
				for (int x = combinations[a][3]; x <= combinations[a][5]; x++) {
					//this inner loop should be executed exactly 69 times from what I calculated previously
					
					xCount = 0; //1 on board or red player
					oCount = 0; //2 on board or yellow player
					for (int b = 0; b < 4; b++) { //checks a given line
						iVal = i + b * combinations[a][0];
						xVal = x + b * combinations[a][1];
						if (board[iVal][xVal] == 2) {
							oCount += 1;
						}
						else if (board[iVal][xVal] == 1) {
							xCount += 1;
						}
					}
					
					if (xCount == 0) {
						if (oCount == 2 && xCount == 0) { //line of two
							fitness += 2;
						}
						else if (oCount == 3 && xCount == 0) { //line of three
							fitness += 7;
							
							for (int b = 0; b < 4; b++) {
								iVal = i + b * combinations[a][0];
								xVal = x + b * combinations[a][1];
								if (board[iVal][xVal] == 0) {
									if (iVal != 5) {
										if (board[iVal + 1][xVal] == 0) {
											fitness += 10;
										}
									}
									break;
								}
							}
						}
						else if (oCount == 4) {
							fitness += 10000000;
						}
					}
					else if (oCount == 0) {
						if (xCount == 2) {
							fitness -= 4;
						}
						else if (xCount == 3) { //Not necessarily winnable - come back to this later
							fitness -= 10; //if just 3 in a row
							
							for (int b = 0; b < 4; b++) {
								iVal = i + b * combinations[a][0];
								xVal = x + b * combinations[a][1];
								if (board[iVal][xVal] == 0) {
									if (iVal != 5) {
										if (board[iVal + 1][xVal] != 0) {
											fitness -= 100000;
										}
										break;
									}
									fitness -= 100000;
									break;
								}
							}
						}
						else if (xCount == 4) { //only used for min/max algorithm
							fitness -= 1000000;
						}
					}
				}
			}
		}
		
		return fitness;
	}
	
	public static int AiMove(int[][] board, int turns) {
		int[][][] possibilities = generateBoards(board, turns);
		int[] fitnesses = new int[possibilities.length];
		for (int i = 0; i < possibilities.length; i++) {
			if (possibilities[i][0][0] == -1) {
				fitnesses[i] = -123456789; //indicates an impossible board hence should be avoided at all costs whether the player is max or min.
			}
			else {
				fitnesses[i] = calculateFitness(possibilities[i]); //turns all boards into a fitness value
			}
		}
		
		return minMax(fitnesses, 0);
	}
	
	public static int minMax(int[] fitnesses, int depth) {
		depth += 1; //depth starts at zero
		int length = fitnesses.length;
		int multiplier = new int[] {-1, 1}[depth % 2]; //relating to min, and max players respectively
		int[] result = new int[length / 7];
		int[] indexes = new int[length / 7];
		for (int i = 0; i < length / 7; i++) {
			result[i] = -123456789 * multiplier; //This was the cause of a previous problem
		}
		
		for (int i = 0; i < length; i++) {
			if (result[i / 7] * multiplier < fitnesses[i] * multiplier) {
				if (fitnesses[i] != -123456789) {
					result[i / 7] = fitnesses[i];
					indexes[i / 7] = i % 7;
				}
			}
		}
		
		for (int i = 0; i < length / 7; i++) {
			if (result[i] == 123456789) {
				result[i] = -123456789;
			}
		}
		
		
		if (result.length > 1) {
			return minMax(result, depth);
		}
		else {
			return indexes[0];
		}
	}
	
	public static int[][][] generateBoards(int[][] board, int turns) { //how many turns to look ahead
		int[] powers = new int[turns];
		int[] indexes = new int[turns];
		powers[0] = 1;
		for (int i = 1; i < turns; i++) {
			powers[i] = 7 * powers[i - 1];
		}
		
		int[][][] allBoards = new int[powers[turns - 1] * 7][][];
		for (int i = 0; i < allBoards.length; i++) {
			allBoards[i] = new int[][] {{-1}}; //-1 indicates a move which is not possible
		}
		recursiveRoutine(allBoards, board, turns, 0, indexes, powers);
		
		return allBoards;
	}
	
	public static void recursiveRoutine(int[][][] allBoards, int[][] currentBoard, int turns, int depth, int[] indexes, int[] powers) {
		depth += 1; //ASSUMES DEPTH STARTS AT 0
		int[][] nextBoard;
		boolean verification;
		for (int i = 0; i < 7; i++) {
			indexes[turns - depth] = i;
			
			nextBoard = cloneBoard(currentBoard);
			verification = false;
			
			if (winCheck(currentBoard, 2) == true || winCheck(currentBoard, 1) == true || currentTurn + depth == 43) { //if a player has won then add no more pieces, however count the board as valid
				verification = true;
			}
			else if (addPiece(nextBoard, (depth % 2) + 1, i) == true) { //if the column is full then the board is no longer valid and it should be discarded
				verification = true;
			}
			
			if (verification == true) { //if no player has won - DONT ADD A NEW PIECE IF A PLAYER HAS WON
				if (depth < turns) {
					recursiveRoutine(allBoards, nextBoard, turns, depth, indexes, powers);
				}
				else { //depth == turns
					int currentIndex = 0;
					for (int x = 0; x < turns; x++) {
						currentIndex += indexes[x] * powers[x];
					}
					allBoards[currentIndex] = cloneBoard(nextBoard);
				}
			}
		}
	}
	
	
	
	public static int[][] cloneBoard(int[][] board) {
		int[][] result = new int[6][7];
		for (int i = 0; i < 6; i++) {
			result[i] = board[i].clone();
		}
		return result;
	}
	
	
	
	
	

	//All of the following is the base game
	
	public static boolean addPiece(int[][] board, int piece, int index) {
		if (index >= 0 && index < 7) {
			for (int i = 5; i >= 0; i--) {
				if (board[i][index] == 0) {
					board[i][index] = piece;
					return true;
				}
			}
		}
		return false;
	}
	
	public static boolean winCheck(int[][] board, int piece) {
		int[][] combinations = new int[][] {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
		int iVal, xVal;
		for (int i = 0; i < 6; i++) {
			for (int x = 0; x < 7; x++) {
				if (board[i][x] == (piece)) { //locates pieces
					
					for (int a = 0; a < 4; a++) { //checks if won
						for (int b = 1; b < 4; b++) {
							iVal = i + (b * combinations[a][0]);
							xVal = x + (b * combinations[a][1]);
							if (iVal >= 0 && iVal < 6 && xVal >= 0 && xVal < 7) {
								if (board[iVal][xVal] != piece) {
									break;
								}
							}
							else {
								break;
							}
							if (b == 3) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}
	
	
	//All of the following was added when graphics were first added
	@Override
	public void create () {
		sr = new ShapeRenderer();
		batch = new SpriteBatch();
		font = new BitmapFont();
		font.getData().setScale(5);
		
		float xInc = xRes / 7, yInc = yRes / 6; //Distance between adjacent holes
		centres = new float[6][7][2];
		for (int i = 0; i < 6; i++) {
			for (int x = 0; x < 7; x++) {
				centres[5 - i][x][0] = (float) ((x + 0.5) * xInc); //5 - i is here to ensure that the grid is not upside down
				centres[5 - i][x][1] = (float) ((i + 0.5) * yInc);
			}
		}
		clearBoard();
	}
	
	public void clearBoard() {
		board = new int[6][7];
		for (int i = 0; i < 6; i++) {
			for (int x = 0; x < 7; x++) {
				board[i][x] = 0;
			}
		}
		Random rand = new Random();
		first = rand.nextInt(2); //determines which player will go first
	}
	
	public int input() {
		int result = -1;
		if (Gdx.input.isButtonPressed(Buttons.LEFT)) {
			if (isHeld == false) {
				result = (int)(Gdx.input.getX() * 7 / xRes);
				isHeld = true;
			}
		}
		else {
			isHeld = false;
		}
		
		if (currentTurn == -1) {
			if (Gdx.input.isKeyJustPressed(Keys.RIGHT)) {
				if (difficulty != 4) {
					difficulty += 1;
				}
			}
			else if (Gdx.input.isKeyJustPressed(Keys.LEFT)) {
				if (difficulty != 1) {
					difficulty -= 1;
				}
			}
			
			if (Gdx.input.isKeyJustPressed(Keys.ENTER)) { //turns ai on or off
				AI = !AI;
			}
		}
		
		if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
			Gdx.app.exit();
		}
		
		return result;
	}
	
	public void renderContents() {
		batch.begin();
		font.getData().setScale(2);
		font.draw(batch, "Left Click - Place counter \n" +
				 "Enter - Toggle AI on / off \n" +
				 "BackSpace - Forfeit game \n" +
				 "Left - Decrease AI difficulty \n" +
				 "Right - Increase AI difficulty \n" +
				 "ESC - Quit game", xRes + 20, yRes);
		font.getData().setScale(5);
		
		if (currentTurn != -1) {
			font.draw(batch, players[(currentTurn + first) % 2] + "'s turn", xRes + 20, yRes / 2);
		}
		else {
			font.draw(batch, output, xRes + 20, yRes / 2);
		}
		
		font.draw(batch, "AI: " + AI, xRes + 20, 160);
		font.draw(batch, "Difficulty: " + difficulty, xRes + 20, 80);
		batch.end();
		
		sr.begin(ShapeType.Filled);
		sr.setColor(0, 0, 1, 1);		
		sr.rect(0, 0, xRes, yRes);
		int[][] colours = {{0, 0, 0}, {1, 0, 0}, {1, 1, 0}}; //colour of no piece, red piece, and yellow piece respectively
		int currentPiece;
		for (int i = 0; i < 6; i++) {
			for (int x = 0; x < 7; x++) {
				currentPiece = board[i][x];
				sr.setColor(colours[currentPiece][0], colours[currentPiece][1], colours[currentPiece][2], 1);
				sr.circle(centres[i][x][0], centres[i][x][1], 90);
			}
		}
		sr.end();
	}

	@Override
	public void render () {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		if (currentTurn != -1) {
			int currentPlayer = ((currentTurn + first) % 2) + 1;
			
			if (currentPlayer == 1 || AI == false) { //if player
				int input = input();
				if (input != -1) {
					if (addPiece(board, currentPlayer, input) == true) {
						currentTurn += 1;
					}
				}
			}
			else { //if AI
				addPiece(board, currentPlayer, AiMove(board, (difficulty * 2) - 1));
				currentTurn += 1;
			}
			
			if (winCheck(board, currentPlayer) == true) {
				currentTurn = -1;
				output = players[currentPlayer - 1] + " wins!";
			}
			
			if (currentTurn == 42) {
				currentTurn = -1;
				output = "Tie";
			}
			
			if (Gdx.input.isKeyJustPressed(Keys.BACKSPACE)) {
				currentTurn = -1;
				output = "Quit";
			}
		}
		else { //if the game is over
			
			if (input() != -1) {
				clearBoard();
				currentTurn = 0;
			}
		}
		
		renderContents();
	}
	
	@Override
	public void dispose () {
		sr.dispose();
		font.dispose();
		batch.dispose();
	}
}
