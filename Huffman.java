// Yasna Karimi
// 300312772

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;
import net.datastructures.*;

/**
 * Class Huffman that provides Huffman compression encoding and decoding of files
 * @author Lucia Moura
 *
 */

public class Huffman {

	/**
	 * 
	 * Inner class Huffman Node to Store a node of Huffman Tree
	 *
	 */
	private class HuffmanTreeNode { 
	    private int character;      // character being represented by this node (applicable to leaves)
	    private int count;          // frequency for the subtree rooted at node
	    private HuffmanTreeNode left;  // left/0  subtree (NULL if empty)
	    private HuffmanTreeNode right; // right/1 subtree subtree (NULL if empty)
	    public HuffmanTreeNode(int c, int ct, HuffmanTreeNode leftNode, HuffmanTreeNode rightNode) {
	    	character = c;
	    	count = ct;
	    	left = leftNode;
	    	right = rightNode;
	    }
	    public int getChar() { return character;}
	    public Integer getCount() { return count; }
	    public HuffmanTreeNode getLeft() { return left;}
	    public HuffmanTreeNode getRight() { return right;}
		public boolean isLeaf() { return left==null ; } // since huffman tree is full; if leaf=null so must be right
	}
	
	/**
	 * 
	 * Auxiliary class to write bits to an OutputStream
	 * Since files output one byte at a time, a buffer is used to group each output of 8-bits
	 * Method close should be invoked to flush half filed buckets by padding extra 0's
	 */
	private class OutBitStream {
		OutputStream out;
		int buffer;
		int buffCount;
		public OutBitStream(OutputStream output) { // associates this to an OutputStream
			out = output;
			buffer=0;
			buffCount=0;
		}
		public void writeBit(int i) throws IOException { // write one bit to Output Stream (using byte buffer)
		    buffer=buffer<<1;
		    buffer=buffer+i;
		    buffCount++;
		    if (buffCount==8) { 
		    	out.write(buffer); 
		    	//System.out.println("buffer="+buffer);
		    	buffCount=0;
		    	buffer=0;
		    }
		}
		
		public void close() throws IOException { // close output file, flushing half filled byte
			if (buffCount>0) { //flush the remaining bits by padding 0's
				buffer=buffer<<(8-buffCount);
				out.write(buffer);
			}
			out.close();
		}
		
 	}
	
	/**
	 * 
	 * Auxiliary class to read bits from a file
	 * Since we must read one byte at a time, a buffer is used to group each input of 8-bits
	 * 
	 */
	private class InBitStream {
		InputStream in;
		int buffer;    // stores a byte read from input stream
		int buffCount; // number of bits already read from buffer
		public InBitStream(InputStream input) { // associates this to an input stream
			in = input;
			buffer=0; 
			buffCount=8;
		}
		public int readBit() throws IOException { // read one bit to Output Stream (using byte buffer)
			if (buffCount==8) { // current buffer has already been read must bring next byte
				buffCount=0;
				buffer=in.read(); // read next byte
				if (buffer==-1) return -1; // indicates stream ended
			}
			int aux=128>>buffCount; // shifts 1000000 buffcount times so aux has a 1 is in position of bit to read
			//System.out.println("aux="+aux+"buffer="+buffer);
			buffCount++;
			if ((aux&buffer)>0) return 1; // this checks whether bit buffcount of buffer is 1
			else return 0;
			
		}

	}
	
	/**
	 * Builds a frequency table indicating the frequency of each character/byte in the input stream
	 * @param input is a file where to get the frequency of each character/byte
	 * @return freqTable a frequency table must be an ArrayList<Integer? such that freqTable.get(i) = number of times character i appears in file 
	 *                   and such that freqTable.get(256) = 1 (adding special character representing"end-of-file")
	 * @throws IOException indicating errors reading input stream
	 */
	
	 private ArrayList<Integer> buildFrequencyTable(InputStream input) throws IOException {
		ArrayList<Integer> freqTable = new ArrayList<>(257);
		for (int i = 0; i < 257; i++) freqTable.add(0); // Initialize frequencies to 0
	
		int byteRead;
		while ((byteRead = input.read()) != -1) {
			freqTable.set(byteRead, freqTable.get(byteRead) + 1);
		}
		freqTable.set(256, 1); // Add special end-of-file character frequency
		input.close();
		return freqTable;
	}
	

	/**
	 * Create Huffman tree using the given frequency table; the method requires a heap priority queue to run in O(nlogn) where n is the characters with nonzero frequency
	 * @param freqTable the frequency table for characters 0..255 plus 256 = "end-of-file" with same specs are return value of buildFrequencyTable
	 * @return root of the Huffman tree build by this method
	 */
	private HuffmanTreeNode buildEncodingTree(ArrayList<Integer> freqTable) {
		PriorityQueue<HuffmanTreeNode> queue = new PriorityQueue<>(Comparator.comparingInt(HuffmanTreeNode::getCount));

		for (int i = 0; i < freqTable.size(); i++) {
			if (freqTable.get(i) > 0) {
				queue.add(new HuffmanTreeNode(i, freqTable.get(i), null, null));
			}
		}

		while (queue.size() > 1) {
			HuffmanTreeNode left = queue.poll();
			HuffmanTreeNode right = queue.poll();
			HuffmanTreeNode parent = new HuffmanTreeNode(-1, left.getCount() + right.getCount(), left, right);
			queue.add(parent);
		}

		return queue.poll(); // Root of the Huffman Tree
	}
	
	/**
	 * 
	 * @param encodingTreeRoot - input parameter storing the root of the HUffman tree
	 * @return an ArrayList<String> of length 257 where code.get(i) returns a String of 0-1 correspoding to each character in a Huffman tree
	 *                                                  code.get(i) returns null if i is not a leaf of the Huffman tree
	 */
	private ArrayList<String> buildEncodingTable(HuffmanTreeNode root) {
		ArrayList<String> codeTable = new ArrayList<>(257);
		for (int i = 0; i < 257; i++) codeTable.add(null);
	
		buildCodes(root, "", codeTable);
		return codeTable;
	}
	
	private void buildCodes(HuffmanTreeNode node, String code, ArrayList<String> codeTable) {
		if (node.isLeaf()) {
			codeTable.set(node.getChar(), code);
		} else {
			buildCodes(node.getLeft(), code + "0", codeTable);
			buildCodes(node.getRight(), code + "1", codeTable);
		}
	}	
	
	/**
	 * Encodes an input using encoding Table that stores the Huffman code for each character
	 * @param input - input parameter, a file to be encoded using Huffman encoding
	 * @param encodingTable - input parameter, a table containing the Huffman code for each character
	 * @param output - output paramter - file where the encoded bits will be written to.
	 * @throws IOException indicates I/O errors for input/output streams
	 */
	private void encodeData(InputStream input, ArrayList<String> encodingTable, OutputStream output) throws IOException {
		OutBitStream bitStream = new OutBitStream(output);
		int byteRead;
	
		while ((byteRead = input.read()) != -1) {
			String code = encodingTable.get(byteRead);
			for (char bit : code.toCharArray()) {
				bitStream.writeBit(bit == '1' ? 1 : 0);
			}
		}
	
		// Write end-of-file code
		String eofCode = encodingTable.get(256);
		for (char bit : eofCode.toCharArray()) {
			bitStream.writeBit(bit == '1' ? 1 : 0);
		}
		bitStream.close();
		input.close();
	}
	
	/**
	 * Decodes an encoded input using encoding tree, writing decoded file to output
	 * @param input  input parameter a stream where header has already been read from
	 * @param encodingTreeRoot input parameter contains the root of the Huffman tree
	 * @param output output parameter where the decoded bytes will be written to 
	 * @throws IOException indicates I/O errors for input/output streams
	 */
	private void decodeData(ObjectInputStream input, HuffmanTreeNode root, OutputStream output) throws IOException {
		InBitStream bitStream = new InBitStream(input);
		HuffmanTreeNode currentNode = root;
		int bit;
	
		while ((bit = bitStream.readBit()) != -1) {
			currentNode = (bit == 0) ? currentNode.getLeft() : currentNode.getRight();
	
			if (currentNode.isLeaf()) {
				if (currentNode.getChar() == 256) break; // End-of-file character
				output.write(currentNode.getChar());
				currentNode = root;
			}
		}
		output.close();
	}
	
	/**
	 * Encodes an input using encoding Table that stores the Huffman code for each character
	 * @param inputFileName - name of the file to be encoded using Huffman encoding
	 * @param outputFileName - name of the file where the encoded bits will be written to
	 * @throws IOException indicates problems with input/output streams
	 */
	public void encode(String inputFileName, String outputFileName) throws IOException {
		System.out.println("\nEncoding " + inputFileName + " " + outputFileName);
	
		// Prepare input and output files streams
		FileInputStream input = new FileInputStream(inputFileName);
		FileInputStream copyinput = new FileInputStream(inputFileName); // create copy to read input twice
		FileOutputStream out = new FileOutputStream(outputFileName);
		ObjectOutputStream codedOutput = new ObjectOutputStream(out); // use ObjectOutputStream to print objects to file
	
		ArrayList<Integer> freqTable = buildFrequencyTable(input); // Build frequencies from input
		HuffmanTreeNode root = buildEncodingTree(freqTable); // Build tree using frequencies
		ArrayList<String> codes = buildEncodingTable(root);  // Build codes for each character in file
	
		codedOutput.writeObject(freqTable); // Write header with frequency table
		encodeData(copyinput, codes, codedOutput); // Write the Huffman encoding of each character in file
	
		// Print file sizes
		File inputFile = new File(inputFileName);
		File outputFile = new File(outputFileName);
		System.out.println("Number of bytes in input : " + inputFile.length());
		System.out.println("Number of bytes in output: " + outputFile.length());
	
		// Close streams
		codedOutput.close();
		copyinput.close();
	}
	
	/**
	 * Decodes an encoded input using encoding tree, writing decoded file to output
	 * @param inputFileName  - name of the file encoded via the encode algorithm of this class 
	 * @param outputFileName - name of the output file where the decoded bytes will be written
	 * @throws IOException indicates problems with input/output streams
	 * @throws ClassNotFoundException handles case where the file does not contain correct object at header
	 */
	public void decode(String inputFileName, String outputFileName) throws IOException, ClassNotFoundException {
		System.out.println("\nDecoding " + inputFileName + " " + outputFileName);
	
		// Prepare input and output file streams
		FileInputStream in = new FileInputStream(inputFileName);
		ObjectInputStream codedInput = new ObjectInputStream(in);
		FileOutputStream output = new FileOutputStream(outputFileName);
	
		ArrayList<Integer> freqTable = (ArrayList<Integer>) codedInput.readObject(); // Read header with frequency table
		HuffmanTreeNode root = buildEncodingTree(freqTable);
		decodeData(codedInput, root, output);
	
		// Print file sizes
		File inputFile = new File(inputFileName);
		File outputFile = new File(outputFileName);
		System.out.println("Number of bytes in input (compressed): " + inputFile.length());
		System.out.println("Number of bytes in output (decompressed): " + outputFile.length());
	
		// Close streams
		codedInput.close();
		output.close();
	}	
}