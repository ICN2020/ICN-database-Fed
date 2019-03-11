// Author : Andrea Detti
// Extended SegmentFetcher with Fixed Tx Window  

package com.ogb.fes.ndn.query;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.NetworkNack;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnNetworkNack;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.OnDataValidationFailed;
import net.named_data.jndn.security.OnVerified;
import net.named_data.jndn.security.SecurityException;
//import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.util.Blob;


public class NDNFixedWindowSegmentFetcher implements OnData, OnTimeout, OnNetworkNack {
	public enum ErrorCode {
		INTEREST_TIMEOUT,
		DATA_HAS_NO_SEGMENT,
		SEGMENT_VERIFICATION_FAILED,
		IO_ERROR
	}

	public interface OnComplete {
		void onComplete(Blob content);
	}

	public interface VerifySegment {
		boolean verifySegment(Data data);
	}

	public interface OnError {
		void onError(ErrorCode errorCode, String message);
	}

	public interface OnNetworkNack {
		/**
		 * When a network Nack packet is received, onNetworkNack is called.
		 * @param interest The interest given to Face.expressInterest. NOTE: You must
		 * not change the interest object - if you need to change it then make a copy.
		 * @param networkNack The received NetworkNack object.
		 */
		void onNetworkNack(Interest interest, NetworkNack networkNack);
	}

	/**
	 * DontVerifySegment may be used in fetch to skip validation of Data packets.
	 */
	public static final VerifySegment DontVerifySegment = new VerifySegment() {
		public boolean verifySegment(Data data) {
			return true;
		}};

	public static final VerifySegment VerifySegment = new VerifySegment() {
		boolean check = false;
		public boolean verifySegment(Data data) {
			KeyChain keyChain = com.ogb.fes.filesystem.FileManager.getKeyChainFromConfigFileName();
			try {
				keyChain.verifyData(data, new OnVerified() {
					@Override
					public void onVerified(Data arg0) {
						System.out.println("Segment verified");
						check = true;
					}
				}, new OnDataValidationFailed() {
					
					@Override
					public void onDataValidationFailed(Data arg0, String arg1) {
						check = false;
					}
				});
			} catch (SecurityException e) {
				e.printStackTrace();
				return check;
			}
			return check;
		}};

		/**
		 * Initiate segment fetching. For more details, see the documentation for
		 * the class.
		 * @param face This calls face.expressInterest to fetch more segments.
		 * @param baseInterest An Interest for the initial segment of the requested
		 * data, where baseInterest.getName() has the name prefix.
		 * This interest may include a custom InterestLifetime and selectors that will
		 * propagate to all subsequent Interests. The only exception is that the
		 * initial Interest will be forced to include selectors "ChildSelector=1" and
		 * "MustBeFresh=true" which will be turned off in subsequent Interests.
		 * @param verifySegment When a Data packet is received this calls
		 * verifySegment.verifySegment(data). If it returns false then abort fetching
		 * and call onError.onError with ErrorCode.SEGMENT_VERIFICATION_FAILED. If
		 * data validation is not required, use DontVerifySegment.
		 * NOTE: The library will log any exceptions thrown by this callback, but for
		 * better error handling the callback should catch and properly handle any
		 * exceptions.
		 * @param onComplete When all segments are received, call
		 * onComplete.onComplete(content) where content is the concatenation of the
		 * content of all the segments.
		 * NOTE: The library will log any exceptions thrown by this callback, but for
		 * better error handling the callback should catch and properly handle any
		 * exceptions.
		 * @param onError Call onError.onError(errorCode, message) for timeout or an
		 * error processing segments.
		 * NOTE: The library will log any exceptions thrown by this callback, but for
		 * better error handling the callback should catch and properly handle any
		 * exceptions.
		 */
		public static void
		fetch
		(Face face, Interest baseInterest, VerifySegment verifySegment,
				OnComplete onComplete, OnError onError)
		{
			new NDNFixedWindowSegmentFetcher(face, verifySegment, onComplete, onError)
			.fetchFirstSegment(baseInterest);
		}

		/**
		 * Create a new SegmentFetcher to use the Face.
		 * @param face This calls face.expressInterest to fetch more segments.
		 * @param verifySegment When a Data packet is received this calls
		 * verifySegment.verifySegment(data). If it returns false then abort fetching
		 * and call onError.onError with ErrorCode.SEGMENT_VERIFICATION_FAILED.
		 * @param onComplete When all segments are received, call
		 * onComplete.onComplete(content) where content is the concatenation of the
		 * content of all the segments.
		 * @param onError Call onError.onError(errorCode, message) for timeout or an
		 * error processing segments.
		 */
		private NDNFixedWindowSegmentFetcher
		(Face face, VerifySegment verifySegment, OnComplete onComplete, OnError onError)
		{
			face_ = face;
			verifySegment_ = verifySegment;
			onComplete_ = onComplete;
			onError_ = onError;
			lastRequestedSegment = 0;
			lastSegmentFetched=false;

		}

		private void
		fetchFirstSegment(Interest baseInterest)
		{
			Interest interest = new Interest(baseInterest);
			
			if (!endsWithSegmentNumber(baseInterest.getName())) {
                            interest.setName(baseInterest.getName().appendSegment(0));
			} else {
                            interest.setName(baseInterest.getName().getPrefix(-1).appendSegment(0));
                        }
                        
			interest.setChildSelector(0);
			interest.setMustBeFresh(true);

			try {
				face_.expressInterest(interest, this, this,this);
				inFlight++;
				lastRequestedSegment=0;
			} catch (IOException ex) {
				try {
					onError_.onError
					(ErrorCode.IO_ERROR, "I/O error fetching the first segment " + ex);
				} catch (Throwable exception) {
					logger_.log(Level.SEVERE, "Error in onError", exception);
				}
			}
		}

		private void
		fetchNextSegment(Interest originalInterest, Name dataName)
		{
			while(inFlight<windowSize_ && !lastSegmentFetched && (lastRequestedSegment<finalSegmentNumber))
			{
				// Start with the original Interest to preserve any special selectors.
				Interest interest = new Interest(originalInterest);
				// Changing a field clears the nonce so that the library will generate a new one.
				interest.setChildSelector(0);
				interest.setMustBeFresh(true);
				interest.setName(dataName.getPrefix(-1).appendSegment(lastRequestedSegment + 1));
				try {
					face_.expressInterest(interest, this, this,this);
					inFlight++;
					lastRequestedSegment++;

				} catch (IOException ex) {
					try {
						onError_.onError
						(ErrorCode.IO_ERROR, "I/O error fetching the next segment " + ex);
					} catch (Throwable exception) {
						logger_.log(Level.SEVERE, "Error in onError", exception);
					}
				}
			}
		}

		public void
		onData(Interest originalInterest, Data data)
		{
			inFlight--;
			boolean verified = false;
			try {
				verified = verifySegment_.verifySegment(data);
			} catch (Throwable ex) {
				logger_.log(Level.SEVERE, "Error in verifySegment", ex);
			}
			if (!verified) {
				try {
					onError_.onError
					(ErrorCode.SEGMENT_VERIFICATION_FAILED, "Segment verification failed");
				} catch (Throwable ex) {
					logger_.log(Level.SEVERE, "Error in onError", ex);
				}
				return;
			}

			if (!endsWithSegmentNumber(data.getName())) {
				// We don't expect a name without a segment number.  Treat it as a bad packet.
				try {
					onError_.onError
					(ErrorCode.DATA_HAS_NO_SEGMENT,
							"Got an unexpected packet without a segment number: " + data.getName().toUri());
				} catch (Throwable ex) {
					logger_.log(Level.SEVERE, "Error in onError", ex);
				}
			}
			else {
				long currentSegment;
				try {
					currentSegment = data.getName().get(-1).toSegment();
				}
				catch (EncodingException ex) {
					try {
						onError_.onError
						(ErrorCode.DATA_HAS_NO_SEGMENT,
								"Error decoding the name segment number " +
										data.getName().get(-1).toEscapedString() + ": " + ex);
					} catch (Throwable exception) {
						logger_.log(Level.SEVERE, "Error in onError", exception);
					}
					return;
				}

				contentParts_.put(currentSegment, data.getContent());

				if (data.getMetaInfo().getFinalBlockId().getValue().size() > 0) {
					try {
						finalSegmentNumber = data.getMetaInfo().getFinalBlockId().toSegment();
					}
					catch (EncodingException ex) {
						try {
							onError_.onError
							(ErrorCode.DATA_HAS_NO_SEGMENT,
									"Error decoding the FinalBlockId segment number " +
											data.getMetaInfo().getFinalBlockId().toEscapedString() + ": " + ex);
						} catch (Throwable exception) {
							logger_.log(Level.SEVERE, "Error in onError", exception);
						}
						return;
					}

					if (currentSegment == finalSegmentNumber) {
						// This is the last segment of the content.
						lastSegmentFetched=true;
					}
					if (lastSegmentFetched) {
						if (haveAllSegments(finalSegmentNumber)) {

							// Get the total size and concatenate to get content.
							int totalSize = 0;
							for (long i = 0; i < contentParts_.size(); ++i){
								totalSize += (contentParts_.get(i)).size();
							}

							ByteBuffer content = ByteBuffer.allocate(totalSize);
							for (long i = 0; i < contentParts_.size(); ++i)
								content.put((contentParts_.get(i)).buf());
							content.flip();

							try {
								onComplete_.onComplete(new Blob(content, false));
								complete=true;
							} catch (Throwable ex) {
								logger_.log(Level.SEVERE, "Error in onComplete", ex);
							}

							return;
						}
					}
				}

				// Fetch the next segment.
				if (lastRequestedSegment<finalSegmentNumber) fetchNextSegment(originalInterest, data.getName());
			}
		}


		public void
		onTimeout(Interest interest)
		{
			nRtx++;
			if (nRtx>nMaxTotRtx) {
				inFlight--;
				if (!complete) {
					try {
						onError_.onError
						(ErrorCode.INTEREST_TIMEOUT,
								"Time out for interest " + interest.getName().toUri());
					} catch (Throwable ex) {
						logger_.log(Level.SEVERE, "Error in onError", ex);
					}
				}
			} else {
				// re-exepress interest
				try {
					face_.expressInterest(interest, this, this,this);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		public void onNetworkNack(Interest interest, NetworkNack networkNack) {
			inFlight--;
			if (!complete) {
				try {
					onError_.onError
					(ErrorCode.INTEREST_TIMEOUT,
							"Network Nack for interest " + interest.getName().toUri());
				} catch (Throwable ex) {
					logger_.log(Level.SEVERE, "Error in onError", ex);
				}
			}
		}

		/**
		 * Check if the last component in the name is a segment number.
		 * @param name The name to check.
		 * @return True if the name ends with a segment number, otherwise false.
		 */
		private static boolean
		endsWithSegmentNumber(Name name)
		{
			return name.size() >= 1 && name.get(-1).isSegment();
		}

		private boolean haveAllSegments(long expectedNumberOfSegments) {
			if(contentParts_.size()==expectedNumberOfSegments+1)
				return true;
			return false;
		}

		public static void main(String[] args)  {
			try {
				Face face = new Face();
				System.out.println("Requesting content"+args[0]);
				Interest interest = new Interest(new Name(args[0]));
				interest.setInterestLifetimeMilliseconds(8000);
				final long startTime=System.currentTimeMillis();
				NDNFixedWindowSegmentFetcher.fetch(
						face, interest, NDNFixedWindowSegmentFetcher.DontVerifySegment,
						new NDNFixedWindowSegmentFetcher.OnComplete() {
							public void onComplete(Blob content) {
								System.out.println("content received, time elapsed = " +(System.currentTimeMillis()-startTime));
								System.exit(0);
							}},
						new NDNFixedWindowSegmentFetcher.OnError() {
								public void onError(NDNFixedWindowSegmentFetcher.ErrorCode errorCode, String message) {
									System.out.println("fetch error: "+message);
									System.exit(0);
								}}
						);
				while (true) {
					face.processEvents();
				}
			}
			catch (Exception e) {
				System.out.println("exception: " + e.getMessage());
			}
		}



		// Use a non-template ArrayList so it works with older Java compilers.
		private final HashMap<Long, Blob> contentParts_ = new HashMap<>(); // of Blob
		private final Face face_;
		private final VerifySegment verifySegment_;
		private final OnComplete onComplete_;
		private final OnError onError_;
		private static final Logger logger_ = Logger.getLogger(NDNFixedWindowSegmentFetcher.class.getName());
		private long finalSegmentNumber=Long.MAX_VALUE;
		private int windowSize_ = 8;
		private int inFlight;
		private long lastRequestedSegment;
		private boolean lastSegmentFetched;
		private boolean complete = false;
		private int nRtx = 0;
		private int nMaxTotRtx = 3; // maximum number of possible retransmission


}
