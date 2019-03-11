package com.ogb.fes.ndn.insert;


import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;

import com.ogb.fes.domain.User;
import com.ogb.fes.ndn.NDNKeychainManager;

import net.named_data.jndn.Data;
import net.named_data.jndn.Name;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.util.Blob;
import net.named_data.jndn.util.Common;
import net.named_data.jndn.util.SignedBlob;


//import org.bson.BasicBSONEncoder;
//import org.bson.BSONObject;



public class NDNContentObject 
{
        public class NDNMaxPacketSizeException extends Exception 
        {
                private static final long serialVersionUID = 1343242341L;

                public NDNMaxPacketSizeException(String errorMessage) {
                        super(errorMessage);
                }
                
                @Override
                public String getMessage() {
                        return "NDN Max Packet Size exceded";
                }
        }
        
        public Name   nameURI;
        private Data   data; 
        private byte[] rawContent;
        private byte[] unsignedContent;
        private byte[] signedContent;

        private User     user;
        private KeyChain keyChain;
        private Name     keyLocator;
        
        public NDNContentObject(String content, String name, User user, boolean allowSegment, boolean isFinal) throws Exception {
                super();
                

                //BSONObject       bson    = (BSONObject)com.mongodb.util.JSON.parse(content);
                //BasicBSONEncoder encoder = new BasicBSONEncoder();
                
                this.user       = user;
                this.rawContent = content.getBytes();//encoder.encode(bson);;
                this.nameURI    = new Name(name);
                this.data       = new Data();
                this.keyLocator = new Name(user.getKeyLocator());

                try {
                        this.keyChain = NDNKeychainManager.createKeychain(keyLocator, user.getPrivateKey(), user.getPublicKey());
                } 
                catch (IOException | SecurityException e) {
                        e.printStackTrace();
                }
                
                this.data.setName(nameURI);
                this.data.setContent(new Blob(rawContent));

                if (isFinal) setIsFinalBlock();

                createSignedContent();
                
                if (!allowSegment)
                {
                        //System.out.println("Data size: "+data.getContent().size());
                        //Max NDN Packet Size reached. Throw max size exception 
                        if (rawContent.length > Common.MAX_NDN_PACKET_SIZE) {
                                System.out.println("Max NDN Packet size reached -> Data size: "+data.getContent().size() +" > " +Common.MAX_NDN_PACKET_SIZE);
                                throw new NDNMaxPacketSizeException("Max NDN Packet size reached");
                        }
                }
                
        }
        
        public NDNContentObject(String content, String name, User user, boolean allowSegment, int finalSegmentNumber) throws Exception {
                super();
                

                //BSONObject       bson    = (BSONObject)com.mongodb.util.JSON.parse(content);
                //BasicBSONEncoder encoder = new BasicBSONEncoder();
                
                this.user       = user;
                this.rawContent = content.getBytes();//encoder.encode(bson);;
                this.nameURI    = new Name(name);
                this.data       = new Data();
                this.keyLocator = new Name(user.getKeyLocator());

                try {
                        this.keyChain = NDNKeychainManager.createKeychain(keyLocator, user.getPrivateKey(), user.getPublicKey());
                } 
                catch (IOException | SecurityException e) {
                        e.printStackTrace();
                }
                
                this.data.setName(nameURI);
                this.data.setContent(new Blob(rawContent));

                setIsFinalBlock(finalSegmentNumber);

                createSignedContent();
                
                if (!allowSegment)
                {
                        //System.out.println("Data size: "+data.getContent().size());
                        //Max NDN Packet Size reached. Throw max size exception 
                        if (rawContent.length > Common.MAX_NDN_PACKET_SIZE) {
                                System.out.println("Max NDN Packet size reached -> Data size: "+data.getContent().size() +" > " +Common.MAX_NDN_PACKET_SIZE);
                                throw new NDNMaxPacketSizeException("Max NDN Packet size reached");
                        }
                }
                
        }
        
        public NDNContentObject(String content, String name, User user, boolean allowSegment) throws Exception {
                this(content, name, user, allowSegment, true);
        }
        
       
        
        public NDNContentObject(String content, String name, User user) throws Exception {
                this(content, name, user, false);
        }

        
        public void setIsFinalBlock() {
                this.data.getMetaInfo().setFinalBlockId(this.data.getName().get(-1));
        }
        public void setIsFinalBlock(int finalSegmentNumber) {
                this.data.getMetaInfo().setFinalBlockId(Name.Component.fromSegment(finalSegmentNumber));
        }
        
        public Name getNameURI() {
                return this.nameURI;
        }
        
        public byte[] getSignedContent() {
                return this.signedContent;
        }
        
        public byte[] getUnsignedContent() {
                
                if (this.unsignedContent != null)
                        createUnsignedContent();
                
                return this.unsignedContent;
        }
        
        
        
        private void createSignedContent() {
                
                try {
                        this.keyChain.sign(this.data, this.keyLocator);
                        SignedBlob signed  = this.data.wireEncode();
                        this.signedContent = signed.getImmutableArray();
                } 
                catch (SecurityException e) {
                        e.printStackTrace();
                }
        }

        private void createUnsignedContent() {
                
                SignedBlob unsigned  = this.data.wireEncode();
                this.unsignedContent = unsigned.getImmutableArray();
        }

        
        public ArrayList<NDNContentObject> fragmentAtMaxNDNPacketSize() throws Exception {
                
                ArrayList<NDNContentObject> result = new ArrayList<NDNContentObject>();

                int  size           = Common.MAX_NDN_PACKET_SIZE-800;
                byte subData[]      = new byte[size];
                int  readByte       = 0;
                int  snCounter      = 0;
                int  totalReadBytes = 0;
                
                //RawContent Length is minor or less then the max ndn packet size. No fragmentation needed
//                 System.out.println("this.rawContent.length "+ rawContent.length);
                if (rawContent.length <= size) {
                        appendSegment(snCounter);
                        if (totalReadBytes == rawContent.length)
                                setIsFinalBlock();
                        result.add(this);
                        return result;
                }
                
                int nSegments=(int)Math.floor(1.0*rawContent.length/size);
                for (byte b : rawContent) {
                        subData[readByte] = b;
                        readByte++;
                        totalReadBytes++;
                        
                        if (readByte == size) {
//                                 System.out.println("Creating ndn content of " + readByte + " bytes");
                                Name nameURI_new = nameURI.getSubName(0, nameURI.size()-1);
                                nameURI_new.appendSegment(snCounter);
                                NDNContentObject ndnContentObject;
                                //System.out.println("ndnContentObject name" + ndnContentObject.nameURI.toUri());
                                ndnContentObject = new NDNContentObject(new String(subData), nameURI_new.toUri(), user,false, nSegments);
                                result.add(ndnContentObject);
                                subData  = new byte[size];
                                readByte = 0;
                                snCounter++;
                        }
                }
                
                if (readByte > 0) {
                        
                        byte[] lastBytes = Arrays.copyOfRange(subData, 0, readByte);
//                         System.out.println("Creating ndn content of " + lastBytes.length + " bytes");
                        Name nameURI_new = nameURI.getSubName(0, nameURI.size()-1);
                        nameURI_new.appendSegment(snCounter);
                        NDNContentObject ndnContentObject;
                        ndnContentObject = new NDNContentObject(new String(lastBytes), nameURI_new.toUri(), user,false, nSegments);
                        //System.out.println("ndnContentObject name" + ndnContentObject.nameURI.toUri());
                        result.add(ndnContentObject);
                }
        
                return result;
        }
        
        public void appendSegment(long snCounter) {
                
                nameURI = nameURI.getSubName(0, nameURI.size()-1);
                nameURI.appendSegment(snCounter);
                data.setName(nameURI);
        }
}
