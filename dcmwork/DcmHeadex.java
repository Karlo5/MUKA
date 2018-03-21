package dcmwork;

import java.io.*;

/**
 * MUKA is a program to Calculate the Monitor Units or the Dose in an independent
 * way of the original Planing System in Radiotherapy Treatments
 *  
 * MUKA is designed to be full DICOM compatible and able to calculate IMRTs (S&S) and use
 * a simplified Collapsed Cone algorithm
 * 
 *  Copyright (C) 2017 Carlos Pino León
 *  
 *  email: carlos.pinoleon@gmail.com
 * 
*	This file is part of MUKA.
*
*	MUKA is free software: you can redistribute it and/or modify
*	it under the terms of the GNU General Public License as published by
*	the Free Software Foundation, either version 3 of the License, or
*	any later version.
*
*	MUKA is distributed in the hope that it will be useful,
*	but WITHOUT ANY WARRANTY; without even the implied warranty of
*	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*	GNU General Public License for more details.
*
*	You should have received a copy of the GNU General Public License
*	along with MUKA.  If not, see <http://www.gnu.org/licenses/>.
**/


public class DcmHeadex {
	
	//DICOM parameters for lists of tags
	private static final int MAXPOINT = 800; //initial dimension of String[] in value(tagt, i, tagi, tagf)
	private static final int MAXNEWSIZE = 750; //maximun ndimension of String[] in value(tagt, i, tagi, tagf)
	
	//DICOM "keys"
    private static final int TRANSFER_SYNTAX_UID = 0x00020010;
    private static final int PIXEL_DATA = 0x7FE00000;

    //Value Representation dictionary
    private static final int AE=0x4145, AS=0x4153, AT=0x4154, CS=0x4353, DA=0x4441, DS=0x4453, DT=0x4454,
        FD=0x4644, FL=0x464C, IS=0x4953, LO=0x4C4F, LT=0x4C54, PN=0x504E, SH=0x5348, SL=0x534C, 
        SS=0x5353, ST=0x5354, TM=0x544D, UI=0x5549, UL=0x554C, US=0x5553, UT=0x5554,
        OB=0x4F42, OW=0x4F57, SQ=0x5351, UN=0x554E, QQ=0x3F3F;

    protected String path;
    
    protected boolean debug = false;
    
    protected static final int ID_OFFSET = 128;  //offset hasta "DICM"
    private static final String DICM = "DICM";
    
    protected BufferedInputStream f;  
    protected int location = 0; 
    private boolean littleEndian = true;
    
    protected int elementLength; 
    private int vr;  // Value Representation
    private static final int IMPLICIT_VR = 0x2D2D; // '--' 
    protected boolean oddLocations; 
    private boolean endfile = false;
    protected boolean bigEndian = false;
    private char[] vrchar = new char[2];

    public static final char[] hexDigits = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};

    //constructor
    public DcmHeadex(String arg)  {
    	this.path = arg;
    }

   
    //getters a nivel de byte
    
    	//1 byte
    int getByte() throws IOException {
        int b = f.read();
        if (b ==-1) {
        	if(debug)System.out.println("end file reached without target");
        	endfile = true;
        }
        ++location;
        return b;
    }
    	//2 bytes ordenados
    int getShort() throws IOException {
        int b0 = getByte();
        int b1 = getByte();
        if (littleEndian)
            return ((b1 << 8) + b0);
        else
            return ((b0 << 8) + b1);
    }
    	//4 bytes ordenados
    int getInt() throws IOException {
        int b0 = getByte();
        int b1 = getByte();
        int b2 = getByte();
        int b3 = getByte();
        if (littleEndian)
            return ((b3<<24) + (b2<<16) + (b1<<8) + b0);
        else
            return ((b0<<24) + (b1<<16) + (b2<<8) + b3);
    }
    	//4 bytes ordenados como float
    float getFloat() throws IOException {
		int b0 = getByte();
		int b1 = getByte();
		int b2 = getByte();
		int b3 = getByte();
		int res = 0;
		if (littleEndian) {
			res += b0;
			res += ( ((long)b1) << 8);
			res += ( ((long)b2) << 16);
			res += ( ((long)b3) << 24);     
		} else {
			res += b3;
			res += ( ((long)b2) << 8);
			res += ( ((long)b1) << 16);
			res += ( ((long)b0) << 24);
		}
		return Float.intBitsToFloat(res);
	}
    
    	//8 bytes ordenados como double
    double getDouble() throws IOException {
		int b0 = getByte();
		int b1 = getByte();
		int b2 = getByte();
		int b3 = getByte();
		int b4 = getByte();
		int b5 = getByte();
		int b6 = getByte();
		int b7 = getByte();
		long res = 0;
		if (littleEndian) {
			res += b0;
			res += ( ((long)b1) << 8);
			res += ( ((long)b2) << 16);
			res += ( ((long)b3) << 24);
			res += ( ((long)b4) << 32);
			res += ( ((long)b5) << 40);
			res += ( ((long)b6) << 48);
			res += ( ((long)b7) << 56);         
		} else {
			res += b7;
			res += ( ((long)b6) << 8);
			res += ( ((long)b5) << 16);
			res += ( ((long)b4) << 24);
			res += ( ((long)b3) << 32);
			res += ( ((long)b2) << 40);
			res += ( ((long)b1) << 48);
			res += ( ((long)b0) << 56);
		}
		return Double.longBitsToDouble(res);
	}
    //obtiene un String de longitud determinada en bytes
    String getString(int length) throws IOException {
        
        String value = new String();
        
        switch (vr) {
			case FD:
				if (elementLength==8)
					value = Double.toString(getDouble());
				else
					for (int i=0; i<elementLength; i++) getByte();
				break;
			case FL:
				if (elementLength==4)
					value = Float.toString(getFloat());
				else
					for (int i=0; i<elementLength; i++) getByte();
				break;
				//case UT:
				//throw new IOException("DcmHeadex not read UT (unlimited text) DICOMs");
		
			case US:
				if (elementLength==2)
					value = Integer.toString(getShort());
				else {
					int n = elementLength/2;
					StringBuilder sb = new StringBuilder();
					for (int i=0; i<n; i++) {
						sb.append(Integer.toString(getShort()));
						sb.append(" ");
					}
					value = sb.toString();
				}
				break;
				
			case UL:
				if (elementLength==4)
					value = Integer.toString(getInt());
				else {
					//aqui puede haber error no revisado
					int n = elementLength/2;
					StringBuilder sb = new StringBuilder();
					for (int i=0; i<n; i++) {
						sb.append(Integer.toString(getShort()));
						sb.append(" ");
					}
					value = sb.toString();
				}
				break;
			
			case SS:
				if (elementLength==2)
					value = Integer.toString(getShort());
				else {
					int n = elementLength/2;
					StringBuilder sb = new StringBuilder();
					for (int i=0; i<n; i++) {
						sb.append(Integer.toString(getShort()));
						sb.append(" ");
					}
					value = sb.toString();
				}
				break;
				
			case SL:
				if (elementLength==4)
					value = Integer.toString(getInt());
				else {
					//aqui puede haber error no revisado
					int n = elementLength/2;
					StringBuilder sb = new StringBuilder();
					for (int i=0; i<n; i++) {
						sb.append(Integer.toString(getShort()));
						sb.append(" ");
					}
					value = sb.toString();
				}
				break;
				
			default:
				if(length<0){
					endfile = true;
					break;
				}
				byte[] buf = new byte[length];
				int pos = 0;
				while (pos<length) {
					int count = f.read(buf, pos, length-pos);
					pos += count;
				}
				value = new String(buf);
				location += length;
				break;
        }

        return value;
    }
    
    	// coge la longitud del valor
    int getLength() throws IOException {
        
    	int b0 = getByte();
        int b1 = getByte();
        int b2 = getByte();
        int b3 = getByte();
        
        // We cannot know whether the VR is implicit or explicit
        // without the full DICOM Data Dictionary for public and
        // private groups.
        
        // We will assume the VR is explicit if the two bytes
        // match the known codes. It is possible that these two
        // bytes are part of a 32-bit length for an implicit VR.
        
        //a pesar de que pueda esta en littleEndian el texto en DICOM (getString i.e) siempre sigue orden bigEndian
        //y en este caso vr a pesar de ser un int 4-byte se comporta como dos caracteres 2-byte.
       	vr = (b0<<8) + b1;
       	vrchar[0] = (char)b0;
       	vrchar[1] = (char)b1;       	
       	       	
        switch (vr) {
            case OB: case OW: case SQ: case UN:
                // Explicit VR with 32-bit length if other two bytes are zero
                if ( (b2 == 0) || (b3 == 0) ) return getInt();
                // Implicit VR with 32-bit length
                vr = IMPLICIT_VR;
                if (littleEndian)
                    return ((b3<<24) + (b2<<16) + (b1<<8) + b0);
                else
                    return ((b0<<24) + (b1<<16) + (b2<<8) + b3);     
            case AE: case AS: case AT: case CS: case DA: case DS: case DT:  case FD:
            case FL: case IS: case LO: case LT: case PN: case SH: case SL: case SS:
            case ST: case TM:case UI: case UL: case US: case UT: case QQ:
                // Explicit vr with 16-bit length
                if (littleEndian)
                    return ((b3<<8) + b2);
                else
                    return ((b2<<8) + b3);
            default:
                // Implicit VR with 32-bit length...
                vr = IMPLICIT_VR;
                if (littleEndian)
                    return ((b3<<24) + (b2<<16) + (b1<<8) + b0);
                else
                    return ((b0<<24) + (b1<<16) + (b2<<8) + b3);
           
        }
        
    }

    int getNextTagLen() throws IOException {
        int groupWord = getShort();
        if (groupWord==0x0800 && bigEndian) {
            littleEndian = false;
            groupWord = 0x0008;
        }
        if(debug){
        	//if(littleEndian) System.out.println("Endiandness: littleEndian");
        	//else System.out.println("Endiandness: bigEndian");
        }
        int elementWord = getShort();
        int tag = groupWord<<16 | elementWord;
        elementLength = getLength();
             
        // hack needed to read some GE files
        // The element length must be even!
        if (elementLength==13 && !oddLocations) elementLength = 10; 
        
        // "Undefined" element length.
        // This is a sort of bracket that encloses a sequence of elements.
        if (elementLength==-1)
            elementLength = 0;
        
        return tag;
    }
    
    public String getpath(){
    	return path;
    }

    //Devuelve el valor de un tag unico
    //===========================================================
    public String value(String tagtar) throws IOException {
        
    	long skipCount;
        location = 0; // location reset for each search 
        endfile = false;
        String cur = null;
        
        f = new BufferedInputStream(new FileInputStream(path));
        
        PrintWriter fo = new PrintWriter (new FileWriter("temp/dicombit.txt"));
               
        skipCount = (long)ID_OFFSET;
        while (skipCount > 0) skipCount -= f.skip( skipCount );
        location += ID_OFFSET;
        vr = IMPLICIT_VR;
        
        if (!getString(4).equals(DICM)) {
        	if(debug) System.out.println("No dicom");
            f.close();
            f = new BufferedInputStream(new FileInputStream(path));
            location = 0;    
        } else {
            if(debug) System.out.println("DCM head recogniced");
        }
        
        //System.out.println(tagtar);
        boolean decodingTags = true;
        
        //bucle de busqueda
        while (decodingTags&&!endfile) {
        	
            int tag = getNextTagLen();
            if ((location&1)!=0) // DICOM tags must be at even locations
                oddLocations = true;
            String s;
            //tag que indica como leer (littleEndian, bigEndian)
            if(tag==TRANSFER_SYNTAX_UID){
                    s = getString(elementLength);
                    if (s.indexOf("1.2.4")>-1||s.indexOf("1.2.5")>-1) {
                        f.close();
                        fo.close();
                        String msg = "DCM Image compresed.\n \n";
                        msg += "Transfer Syntax UID = "+s;
                        throw new IOException(msg);
                    }
                    if (s.indexOf("1.2.840.10008.1.2.2")>=0)
                        bigEndian = true;
            }
            //tag buscado
            String dummy="";
            byte[] usexception = new byte[2]; //parche para los VR_Implicit US
            if(tagtar.equals(String.format("%08X", tag))){ 
                                        
                // elementLength was reset in previous call to getNextTag()
            	cur = getString(elementLength);
            	if(cur.length()!=0){
            		char end = cur.charAt(cur.length()-1);
            	    String buka = Integer.toHexString(end | 0x10000).substring(1);
            	    if(buka.equals("0000")||buka.equals("0020")) cur = cur.substring(0, cur.length()-1);
            	    dummy = cur;
            	}
            	//parche para VR_IMPLICIT valores US del TAG 0028xxxx
                if ((vr==IMPLICIT_VR)&&(String.format("%08X", tag).startsWith("0028"))&&(elementLength==2)){
                	usexception = cur.getBytes();
                	int b0 = usexception[0];
                	int b1 = 0;
                	if(cur.getBytes().length==2) b1 = usexception[1];
                	int bs = 0;
                	if (littleEndian) bs = ((b1 << 8) + b0);
                    else bs = ((b0 << 8) + b1);
                	cur = String.valueOf(Integer.toString(bs));
                	dummy = cur;
                }
                //fin parche
            	
            	
            }else{
            	if(!(tag==TRANSFER_SYNTAX_UID)){
            		dummy = getString(elementLength);
            	}
            }
            //tag de inicio de pixel data. Final
            if (tag==PIXEL_DATA) endfile = true;

            if(debug&&!endfile){
            	            
            	fo.print("long: ");
            	fo.print(elementLength);
            	fo.print(", tag: ");
            	fo.print(String.format("%08X", tag));
            	fo.print(", VR: ");
            	String vrstring = new String(vrchar);
            	if(vr==IMPLICIT_VR) fo.print("XX"); 
            	else fo.print(vrstring);
            	fo.print(", value: ");
            	fo.println(dummy);
            }
            
        } // while(decodingTags)

        f.close();
        fo.close();
        return cur;
        
    }
    
    
    //Devuelve el valor i-esimo de un tag que se repite
    //===========================================================
    public String value(String tagtar, int iex) throws IOException {
        
    	long skipCount;
        location = 0; // location reset for each search 
        endfile = false;
        String cur = null;
        int ifound = 0;
        
        f = new BufferedInputStream(new FileInputStream(path));
        
        PrintWriter fo = new PrintWriter (new FileWriter("temp/dicombit.txt"));
               
        skipCount = (long)ID_OFFSET;
        while (skipCount > 0) skipCount -= f.skip( skipCount );
        location += ID_OFFSET;
        vr = IMPLICIT_VR;
        
        if (!getString(4).equals(DICM)) {
            f.close();
            f = new BufferedInputStream(new FileInputStream(path));
            location = 0;    
        } else {
            //System.out.println("Se reconoce cabecera DCM");
        }
        
        boolean decodingTags = true;
        
        //bucle de busqueda
        while (decodingTags&&!endfile) {
        	
            int tag = getNextTagLen();
            if ((location&1)!=0) // DICOM tags must be at even locations
                oddLocations = true;
            String s;
            if(tag==TRANSFER_SYNTAX_UID){
                    s = getString(elementLength);
                    if (s.indexOf("1.2.4")>-1||s.indexOf("1.2.5")>-1) {
                        f.close();
                        fo.close();
                        String msg = "DCM Image compresed.\n \n";
                        msg += "Transfer Syntax UID = "+s;
                        throw new IOException(msg);
                    }
                    if (s.indexOf("1.2.840.10008.1.2.2")>=0)
                        bigEndian = true;
            }
            String dummy="";
            if(tagtar.equals(String.format("%08X", tag))){ 
                    // elementLength was reset in previous call to getNextTag() 
            		dummy = getString(elementLength);
                    
                    if(ifound==iex){
                    	
                    	char end = dummy.charAt(dummy.length()-1);
           				String buka = Integer.toHexString(end | 0x10000).substring(1);
           				if(buka.equals("0000")||buka.equals("0020")) dummy = dummy.substring(0, dummy.length()-1);
                    	
           				cur = dummy;
                        break;
                    }
                    ifound++;
            }else{
            	if(!(tag==TRANSFER_SYNTAX_UID)){
            		dummy = getString(elementLength);
            	}
            }
            
            boolean debug = false;
            if(debug&&!endfile){
                
            	fo.print("long: ");
            	fo.print(elementLength);
            	fo.print(", tag: ");
            	fo.print(String.format("%08X", tag));
            	fo.print(", VR: ");
            	String vrstring = new String(vrchar);
            	fo.print(vrstring);
            	fo.print(", value: ");
            	fo.println(dummy);
            }
            
        } // while(decodingTags)

        f.close();
        fo.close();
        return cur;
        
    }
    
	//Devuelve todos los valores de valor tags, encontrados entre los tagt y tagf j y j-1
    //===========================================================
    public String[] value(String tagtar, int volj, String tagi, String tagf) throws IOException {
        
    	long skipCount;
        location = 0; // location reset for each search 
        endfile = false;
        String[] bigcur = new String[MAXPOINT];
        bigcur[0] = "";
        int volk = 0;
        int p = 0;
        
        f = new BufferedInputStream(new FileInputStream(path));
        
        PrintWriter fo = new PrintWriter (new FileWriter("temp/dicombit.txt"));
               
        skipCount = (long)ID_OFFSET;
        while (skipCount > 0) skipCount -= f.skip( skipCount );
        location += ID_OFFSET;
        vr = IMPLICIT_VR;
        
        if (!getString(4).equals(DICM)) {
            f.close();
            f = new BufferedInputStream(new FileInputStream(path));
            location = 0;    
        } else {
            //System.out.println("Se reconoce cabecera DCM");
        }
        
        boolean decodingTags = true;
        
        //bucle de busqueda
        while (decodingTags&&!endfile) {
        	
            int tag = getNextTagLen();
            if ((location&1)!=0) // DICOM tags must be at even locations
                oddLocations = true;
            String s;
            
            if(tag==TRANSFER_SYNTAX_UID){
                    s = getString(elementLength);
                    if (s.indexOf("1.2.4")>-1||s.indexOf("1.2.5")>-1) {
                        f.close();
                        fo.close();
                        String msg = "DCM Image compresed.\n \n";
                        msg += "Transfer Syntax UID = "+s;
                        throw new IOException(msg);
                    }
                    if (s.indexOf("1.2.840.10008.1.2.2")>=0)
                        bigEndian = true;
            }
            
            String dummy="";
            
            //============================inicio del meollo
            
            if(tagi.equals(String.format("%08X", tag))&&(volk==volj)){
               		

                //System.out.println("se encuentra "+String.format("%08X", tag)+" con vol "+String.valueOf(volk));
            	
            	dummy = getString(elementLength);
               		
               	while(!tagf.equals(String.format("%08X", tag))){
               			
               			tag = getNextTagLen();
               		
               			if(tagtar.equals(String.format("%08X", tag))){ 
               				// elementLength was reset in previous call to getNextTag()
               				dummy = getString(elementLength);
               				if(dummy.length()>0) {
               					char end = dummy.charAt(dummy.length()-1);
               					String buka = Integer.toHexString(end | 0x10000).substring(1);
                   				if(buka.equals("0000")) dummy = dummy.substring(0, dummy.length()-1);
                   				bigcur[p]= dummy;
                   				p++;
               				}
               				
               			}else{
               				if(!(tag==TRANSFER_SYNTAX_UID)) dummy = getString(elementLength);
               			}
               	}
               	volk++;
               	if(tagf.equals(String.format("%08X", tag))) break;
               	
            }else{
            		if(!(tag==TRANSFER_SYNTAX_UID)) dummy = getString(elementLength);
            		if(tagi.equals(String.format("%08X", tag))) volk++;	
            }
            
            
        } // while(decodingTags)
        
        f.close();
        fo.close();
        
        int newsize=0;
        for (int i=0; i<MAXNEWSIZE;i++){
        	if(bigcur[i]!=null){
        		newsize++;
        	}
        }
        
        String[] cur = new String[newsize];
        
        for (int i=0; i<newsize;i++){
        	cur[i] = bigcur[i];
        }
        
        return cur;
        
    }
    
    
    public void verbose() throws IOException{
    	
       location = 0;

       f = new BufferedInputStream(new FileInputStream(path));
       PrintWriter fo = new PrintWriter (new FileWriter("temp/dicombit.txt"));
       endfile = false;
       
       fo.println("Archivo Dicom en Hexadecimal");
       
       //while(!endfile){
    //		fo.print(String.format("%08X",getNextTagLen())+" ");
    //		fo.print(elementLength);
    //	}
       
       	fo.close();
       	f.close();
    	
    }
}