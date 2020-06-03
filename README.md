# Connect-4-With-Graphics
A modified version of my Connect 4 project which uses LibGDX instead of the command line. The only thing required to run this project is the jar file.  
  
The AI uses my own implementation of the min max algorithm (without any additional optimisation techniques) in order to determine the best move it can make, given that it can predict what the game will look like in several moves time.  
  
The amount of moves the AI can see ahead is equal to (2 * difficulty) - 1, therefore for the maximum difficulty (4) the AI would be able to see 7 moves ahead (equating to 7^7 (823,543) different board possibilities, therefore this may be rather RAM (and CPU) intensive).
