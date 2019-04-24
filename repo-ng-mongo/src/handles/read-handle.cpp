/* -*- Mode:C++; c-file-style:"gnu"; indent-tabs-mode:nil; -*- */
/**
 * Copyright (c) 2014,  Regents of the University of California.
 *
 * This file is part of NDN repo-ng (Next generation of NDN repository).
 * See AUTHORS.md for complete list of repo-ng authors and contributors.
 *
 * repo-ng is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * repo-ng is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * repo-ng, e.g., in COPYING.md file.  If not, see <http://www.gnu.org/licenses/>.
 */

#include "read-handle.hpp"

using std::string;
using bsoncxx::builder::stream::finalize;
using bsoncxx::builder::basic::kvp;

int count =0;
unsigned char *buffer;

int totalQuery = 0;
int voidQuery = 0;

const std::string DB_NAME = "ogb_repo";
//const std::string reponame = "repoEU";

const char HEX2DEC[256] = 
{
    /*       0  1  2  3   4  5  6  7   8  9  A  B   C  D  E  F */
    /* 0 */ -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1,
    /* 1 */ -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1,
    /* 2 */ -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1,
    /* 3 */  0, 1, 2, 3,  4, 5, 6, 7,  8, 9,-1,-1, -1,-1,-1,-1,
    
    /* 4 */ -1,10,11,12, 13,14,15,-1, -1,-1,-1,-1, -1,-1,-1,-1,
    /* 5 */ -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1,
    /* 6 */ -1,10,11,12, 13,14,15,-1, -1,-1,-1,-1, -1,-1,-1,-1,
    /* 7 */ -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1,
    
    /* 8 */ -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1,
    /* 9 */ -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1,
    /* A */ -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1,
    /* B */ -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1,
    
    /* C */ -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1,
    /* D */ -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1,
    /* E */ -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1,
    /* F */ -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1
};

std::string uriDecode(const std::string & sSrc)
{
   // Note from RFC1630: "Sequences which start with a percent
   // sign but are not followed by two hexadecimal characters
   // (0-9, A-F) are reserved for future extension"

   const unsigned char * pSrc = (const unsigned char *)sSrc.c_str();
   const int SRC_LEN = sSrc.length();
   const unsigned char * const SRC_END = pSrc + SRC_LEN;
   // last decodable '%' 
   const unsigned char * const SRC_LAST_DEC = SRC_END - 2;

   char * const pStart = new char[SRC_LEN];
   char * pEnd = pStart;

   while (pSrc < SRC_LAST_DEC)
   {
      if (*pSrc == '%')
      {
         char dec1, dec2;
         if (-1 != (dec1 = HEX2DEC[*(pSrc + 1)])
            && -1 != (dec2 = HEX2DEC[*(pSrc + 2)]))
         {
            *pEnd++ = (dec1 << 4) + dec2;
            pSrc += 3;
            continue;
         }
      }

      *pEnd++ = *pSrc++;
   }

   // the last 2- chars
   while (pSrc < SRC_END)
      *pEnd++ = *pSrc++;

   std::string sResult(pStart, pEnd);
   delete [] pStart;
   return sResult;
  }


namespace repo {

  std::string readDataIdentifier(Name name)
  {
    std::vector<std::string> componentNameVector;
    std::string name_str = name.toUri();
    boost::split(componentNameVector, name_str, boost::is_any_of("/"));
    std::string data_id = "";
    if (componentNameVector.size() > 3)
      data_id  = componentNameVector.at(3);
    return data_id;
  }

  std::string readTenantIdentifier(Name name)
  {
    std::vector<std::string> componentNameVector;
    std::string name_str = name.toUri();
    boost::split(componentNameVector, name_str, boost::is_any_of("/"));
    std::string data_id = "";
    if (componentNameVector.size() > 4)
      data_id  = componentNameVector.at(4);
    return data_id;
  }

  void ReadHandle::sendData(Name nameOrig, std::string content)
  {
    Name name = Name(nameOrig);

    //remove sequence number component if present
    name::Component lastComponent = name.get(name.size()-1);
    //std::size_t found = lastComponent.toUri().find("%");
    //if (found!=std::string::npos)
    if(lastComponent.isSegment())
    {
      name = name.getPrefix(-1); 
    }
    
    //std::cout << "name in sandData: " << name << std::endl;

    //all query are done now send the information
    int MAX_SIZE_PACKET = ndn::MAX_NDN_PACKET_SIZE - 800;
    long n_segments = (long) ceil((double)content.length()/(double)MAX_SIZE_PACKET);
          
    KeyChain l_keyChain;

    //std::cout<< "num segments: " << n_segments << std::endl; 

    //if n_segments == 0 some problem was encountered
    if (n_segments == 0)
    {
      //std::cout<< "n_segments == 0" << std::endl;
      shared_ptr<Data> data = make_shared<Data>();
      data->setName(Name(name).appendSegment(0));
      data->setContent(reinterpret_cast<const uint8_t*>(""), 1);
      data->setFinalBlockId(name::Component::fromSegment(0));
      data->setFreshnessPeriod(milliseconds(0)); 
      l_keyChain.signWithSha256(*data);
      getFace().put(*data);
      return;
    }
    else
    {
      //int remainingSize = totalSize;
      int matchedIndex = 0;
      shared_ptr<Data> dataVector[n_segments];

      for (long i = 0; i < n_segments; i++)
      {
        int segmentSize=MAX_SIZE_PACKET;
        if (i== n_segments-1) {
          segmentSize = content.length()-MAX_SIZE_PACKET*i;
        }
        shared_ptr<Data> data = make_shared<Data>();
        data->setName(Name(name).appendSegment(i));
        data->setContent(reinterpret_cast<const uint8_t*>(content.substr(i*MAX_SIZE_PACKET, segmentSize).c_str()), segmentSize);
        data->setFinalBlockId(name::Component::fromSegment(n_segments-1));
        data->setFreshnessPeriod(milliseconds(0)); 
        l_keyChain.signWithSha256(*data);
        dataVector[i] = data;
        if (data->getName().equals(nameOrig))
        {
          //std::cout<< "Segment == " << i << std::endl;
          matchedIndex=i;
        }
      }

      for (long i = 0; i < n_segments; i++)
      {
        //INSERT data in LRU Cache
        getStorageHandle().lruCache->insert(*dataVector[i]);
      }

      getFace().put(*dataVector[matchedIndex]);
    }
  }

  std::string unzip(unsigned char *compressed_data)
  {

    int unc_data_size=0;
    unc_data_size=unc_data_size+(compressed_data[count-1]<<3*8);
    unc_data_size=unc_data_size+(compressed_data[count-2]<<2*8);
    unc_data_size=unc_data_size+(compressed_data[count-3]<<1*8);
    unc_data_size=unc_data_size+(compressed_data[count-4]);
       
    
    std::cout<<"comp_data size: "<<count<<std::endl; 
    std::cout<<"unc_data size: "<<unc_data_size<<std::endl; 

    unc_data_size=unc_data_size*2;
    
    char uncompressed_data[unc_data_size];
       
    int err;

    // inflate b into c
    // zlib struct
    z_stream infstream;
    infstream.zalloc = Z_NULL;
    infstream.zfree = Z_NULL;
    infstream.opaque = Z_NULL;
    // setup "b" as the input and "c" as the compressed output
    infstream.avail_in  = (uInt)count; // size of input
    infstream.next_in   = (Bytef *)compressed_data; // input char array
    infstream.avail_out = (uInt)unc_data_size; // size of output
    infstream.next_out  = (Bytef *)uncompressed_data; // output char array

    err = inflateInit2(&infstream, 16+MAX_WBITS);
    if (err != Z_OK) return "";

    while (err != Z_STREAM_END) err = inflate(&infstream, Z_NO_FLUSH);

    err = inflateEnd(&infstream);
    
//     std::cout<<"unzip finished: "<<std::endl; 
    
    return std::string(uncompressed_data); 
  }


  void OnBegin( const happyhttp::Response* r, void* userdata )
  {
//     printf( "BEGIN (%d %s)\n", r->getstatus(), r->getreason() );
    count = 0;

    buffer = (unsigned char *)malloc(sizeof(char)*1);
  }

  void OnData( const happyhttp::Response* r, void* userdata, const unsigned char* data, int n )
  {
    //fwrite( data,1,n, stdout );
    buffer = (unsigned char *)realloc(buffer, (count+n)*sizeof(char));
    memcpy(&buffer[count], data, n);
    //sprintf((const char*) &buffer[count], "%s", data);
    count += n;
  }  

  void OnComplete( const happyhttp::Response* r, void* userdata )
  {
    buffer = (unsigned char *)realloc(buffer, (count+1)*sizeof(char));
    buffer[count] = '\0';
    
//       struct timeval tp;
//      gettimeofday(&tp, NULL);
//      long int time1 = tp.tv_sec * 1000 + tp.tv_usec / 1000; //get current timestamp in milliseconds
//    std::cout<<"INSERT_REQUEST - http get complete time:"<<time1<<std::endl;
    
    std::string uncompressed_json = unzip(buffer);

//      gettimeofday(&tp, NULL);
//      long int time2 = tp.tv_sec * 1000 + tp.tv_usec / 1000; //get current timestamp in milliseconds
//      std::cout<<"INSERT_REQUEST - finish decompression time:"<<time2<<std::endl;
    
    ReadHandle* thisHandle = static_cast<ReadHandle*>(userdata);
    thisHandle->makeLongInsert(uncompressed_json);
    
//     std::cout << "xx uncompressed_json size: "<<uncompressed_json.length()<<std::endl;
//     std::cout << "xx uncompressed_json: "<<uncompressed_json<<std::endl;
    
    free(buffer);   
}
  void ReadHandle::makeLongInsert(std::string data_string)
  {
      getStorageHandle().insertLongData(data_string); 
  }

  void ReadHandle::requestInsert(const Interest& interest) 
  {
//     struct timeval tp;
//     gettimeofday(&tp, NULL);
//     long int start = tp.tv_sec * 1000 + tp.tv_usec / 1000; //get current timestamp in milliseconds
//     std::cout<<"INSERT_REQUEST - startTime:"<<start<<std::endl;
    
    std::string insert_tag = "/INSERT_REQUEST";
    Name name = interest.getName();
    std::string decodeName = uriDecode(name.toUri());
    std::size_t pos = decodeName.find(insert_tag);
//     if (pos==std::string::npos)
//     {
      //std::cout << "insert_tag found in position "<< pos <<std::endl; 
      sendData(interest.getName(), ""); 
//     }

    std::string url_string = decodeName.substr(pos+insert_tag.length()+1);
//     std::cout << "requestInsert: " << url_string << std::endl; 


    std::string fes_url, path_str, fes_IP_str, fes_port_str;
    int fes_port;

    auto equals_idx = url_string.find_first_of('/');
    if (std::string::npos != equals_idx)
    {
        fes_url = url_string.substr(0, equals_idx);
        path_str = "/"+url_string.substr(equals_idx + 1);
    }

    equals_idx = fes_url.find_first_of(':');
    if (std::string::npos != equals_idx)
    {   
        fes_IP_str = fes_url.substr(0, equals_idx);
        fes_port_str = fes_url.substr(equals_idx + 1);
    fes_port = std::stoi (fes_port_str);
    }

    std::cout << "URL: " << fes_IP_str << ":" << fes_port_str << path_str << std::endl;

    happyhttp::Connection conn(fes_IP_str.c_str(), fes_port);
    
    conn.setcallbacks( OnBegin, OnData, OnComplete, this );

    conn.request( "GET", path_str.c_str(), 0, 0,0 );

    while( conn.outstanding() )
      conn.pump();

  }


  void ReadHandle::readQuery(const Interest& interest) 
  {

    Name name = interest.getName();

    //struct timeval tp;
    //gettimeofday(&tp, NULL);
    //long int start = tp.tv_sec * 1000 + tp.tv_usec / 1000; //get current timestamp in milliseconds


    //FIND if the data is in LRU Cache
    shared_ptr<const Data> LRUData = getStorageHandle().lruCache->find(name);
    if (LRUData != NULL)
    {
      //std::cout<< name.toUri() << " is CACHED: " << std::endl;
      getFace().put(*LRUData);
      return;
    }

    name::Component lastComponent = name.get(name.size()-1);
    if(lastComponent.isSegment())
    {
      //std::cout << " is a SEGMENT: " << std::endl;
      name = name.getPrefix(-1); 
    }

    std::string decodeName = uriDecode(name.getPrefix(-1).toUri());

    //std::cout << "readQuery: " << decodeName << std::endl;

    std::string data_id  = readDataIdentifier(name);
    std::string tenant_id  = readTenantIdentifier(name);

    if (data_id == "" || tenant_id == "")
    {
      sendData(interest.getName(), "");
      return;
    }

    //std::cout << "tenant_id: " << tenant_id  << std::endl << "data_id: " << data_id << std::endl;

    mongocxx::client conn{mongocxx::uri{}};
    auto collection = conn[tenant_id+"_index"][data_id];

    std::size_t pos = decodeName.find("/QUERY=");
    if (pos==std::string::npos)
    {
      sendData(interest.getName(), ""); 
    }

    std::string query_string = decodeName.substr(pos);
    boost::erase_all(query_string, "/QUERY=");

    boost::replace_all(query_string, "\"geometry", "\"content.geometry");
    //std::cout << "query_string: " << query_string << std::endl;

    std::string result = "";
    
    //gettimeofday(&tp, NULL);
    //long int stop1 = tp.tv_sec * 1000 + tp.tv_usec / 1000; //get current timestamp in milliseconds
    
    //std::cout << "for interest "<<interest.getName().toUri() <<std::endl;  
    //std::cout << "pre_query_time: "<< stop1-start <<" ms" <<std::endl;  
    
    try
    { 
      mongocxx::options::find opts{};
      opts.projection(bsoncxx::builder::stream::document{} << "reference" << 1 <<  "_id" << 0 << finalize);
      auto cursor = collection.find(bsoncxx::from_json(query_string), opts);
      std::vector<std::string> result_vector;
     
      for (auto&& doc : cursor) {
          result_vector.push_back(bsoncxx::to_json(doc));
      }
      
      totalQuery++;
      if(result_vector.size() == 0){
        voidQuery++;
        std::cout << "void percentage: "<< 1.0* voidQuery/totalQuery << " totalQuery: "<<totalQuery<< "voidQuery: "<<voidQuery << std::endl;
      }
      result = boost::algorithm::join(result_vector, ",");

      boost::erase_all(result, "{ \"reference\" : ");
      boost::erase_all(result, " }");
      boost::erase_all(result, "\\"); 
     
      //gettimeofday(&tp, NULL);
      //long int stop2 = tp.tv_sec * 1000 + tp.tv_usec / 1000; //get current timestamp in milliseconds

      //std::cout << "mongo_time: "<< stop2-stop1 <<" ms" << std::endl;  

      //std::cout << std::endl << "For Name: " << interest.getName() << std::endl; 
      //std::cout << "result: " << result << std::endl;  
    }
    catch (...)//bsoncxx::exception& error)
    {
        std::cout << "SOME ERROR IN readQuery! "<< std::endl;// << error.what() << std::endl;
    }

    sendData(interest.getName(), result);

    //gettimeofday(&tp, NULL);
    //long int stop3 = tp.tv_sec * 1000 + tp.tv_usec / 1000; //get current timestamp in milliseconds
    //std::cout << "total_time: "<< stop3-start <<" ms" << std::endl;  
  } 

  void ReadHandle::readVersion(const Interest& interest)
  {

    mongocxx::client conn{mongocxx::uri{}};
    auto collection = conn["index_db"]["index_collection"];

    mongocxx::options::find opts{};
    opts.projection(bsoncxx::builder::stream::document{} << "name" << 1 <<  "_id" << 0 << finalize);
//     std::string query_string = "{\"name\":{\"$ne\":\"INDEX_VERSION\"}}";
    std::string query_string = "{}";

    auto cursor = collection.find(bsoncxx::from_json(query_string), opts);
    std::vector<std::string> result_vector;
   
    for (auto&& doc : cursor) {
        result_vector.push_back(bsoncxx::to_json(doc));
    }

    //int index = 0;
    //for (std::vector<std::string>::const_iterator i = result_vector.begin(); i != result_vector.end(); ++i)
    //   {
    //        std::cout <<index <<")" << *i << std::endl;
    //        index++;
    //   }

    std::string result = boost::algorithm::join(result_vector, ",");

    boost::erase_all(result, "{ \"name\" : ");
    boost::erase_all(result, " }");
    boost::erase_all(result, "\\"); 

    result = "["+result+"]";
    result = "{\"repo_name\": \"/repo/"+reponame+"\",\"prefix\": "+result+"}";
 
//     KeyChain l_keyChain;
//     shared_ptr<Data> data(new Data());
//     data->setContent(reinterpret_cast<const uint8_t*>(result.c_str()), result.size()) ;
//     data->setName(interest.getName());
//     data->setFreshnessPeriod(milliseconds(0)); 
//     l_keyChain.signWithSha256(*data);
//     getFace().put(*data);
    sendData(interest.getName(), result);

  }


  void
  ReadHandle::onInterest(const Name& prefix, const Interest& interest)
  {
    //std::cout << "interest read required: " << interest.getName().toUri()<<std::endl;

    Name name = interest.getName();
 
    std::size_t isQuery = name.toUri().find("/QUERY%3D");
    if (isQuery!=std::string::npos)
    {
      readQuery(interest); 
      return;
    }

    isQuery = name.toUri().find("/INSERT_REQUEST");
    if (isQuery!=std::string::npos)
    { 
      requestInsert(interest);
      return;
    }

    isQuery = name.toUri().find("/DELETE");
    if (isQuery!=std::string::npos)
    {

      getStorageHandle().deleteData(interest.getName());
      KeyChain l_keyChain;
      shared_ptr<Data> data = make_shared<Data>();
      data->setName(interest.getName());
      data->setContent(reinterpret_cast<const uint8_t*>(""), 1);
      data->setFinalBlockId(name::Component::fromSegment(0));
      data->setFreshnessPeriod(milliseconds(0)); 
      l_keyChain.signWithSha256(*data);
      getFace().put(*data);
      return;
    }

    isQuery = name.toUri().find("/GET_PREFIX");
    if (isQuery!=std::string::npos)
    {
      //std::cout << "interest GET PREFIX " <<std::endl;

      readVersion(interest); 
      return;
    }

    shared_ptr<ndn::Data> data = getStorageHandle().readData(interest);
    //std::cout << "before put :" << data->getContent().size() << std::endl;
    if (data != NULL) 
    {
      getFace().put(*data);
    }
      
    
  }


  void
  ReadHandle::onRegisterFailed(const Name& prefix, const std::string& reason)
  {
    std::cerr << "ERROR: Failed to register prefix in local hub's daemon" << std::endl;
    getFace().shutdown();
  }


  void
  ReadHandle::listen(const Name& prefix)
  {
    ndn::InterestFilter filter(prefix);
    getFace().setInterestFilter(filter,
                                bind(&ReadHandle::onInterest, this, _1, _2),
                                bind(&ReadHandle::onRegisterFailed, this, _1, _2));
  }

} //namespace repo
