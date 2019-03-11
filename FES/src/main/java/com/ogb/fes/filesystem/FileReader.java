package com.ogb.fes.filesystem;


import java.io.*;
import java.util.Vector;


public class FileReader
{
	private String fileName;

	//Costruttore
	public FileReader(String fileName)
	{
		this.fileName = fileName;
	}


	//Metodi Get
	public long getFileLength()
	{
		try
		{
			return new File(fileName).length();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return -1;
		}
	}

	public byte[] getAllBytes()
	{
		ByteArrayOutputStream fileBytes = new ByteArrayOutputStream();

		try
		{
			BufferedReader fileInStream = new BufferedReader(new java.io.FileReader(new File(fileName)));

			String line;
			while ( (line = fileInStream.readLine()) != null )
				fileBytes.write(line.getBytes());
			fileInStream.close();

			return fileBytes.toByteArray();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public String[] getAllLines()
	{
		Vector<String> fileLines = new Vector<String>();

		try
		{
			BufferedReader fileInStream = new BufferedReader(new java.io.FileReader(new File(fileName)));

			String line;
			while ( (line = fileInStream.readLine()) != null )
				fileLines.add(line + "\n");
			fileInStream.close();

		    return fileLines.toArray(new String[fileLines.size()]);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public String getAllLinesConcat()
	{
		StringBuffer strFile = new StringBuffer();

		try {
			
			BufferedReader fileInStream = new BufferedReader(new java.io.FileReader(new File(fileName)));

			String line;
			while ( (line = fileInStream.readLine()) != null ) {
				strFile.append(line);
			}
			fileInStream.close();

			return strFile.toString();
		}
		catch(Exception e) {
			
			e.printStackTrace();
			return null;
		}
	}
}
