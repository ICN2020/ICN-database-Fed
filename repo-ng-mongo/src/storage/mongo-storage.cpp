#include "mongo-storage.hpp"


namespace bg = boost::geometry;
typedef bg::model::point<double, 2, bg::cs::cartesian> point_t;
typedef bg::model::polygon<point_t> polygon_t;
typedef bg::model::multi_point<point_t> multi_point_t;
typedef bg::model::box<point_t> box_t;

namespace repo {
  
    using std::string;
    using bsoncxx::builder::stream::finalize;
    using bsoncxx::builder::basic::kvp;


    const std::string DATA_DB_NAME = "data_db";
    const std::string INDEX_DB = "index_db";
    const std::string INDEX_COLLECTION = "index_collection";
    const std::string INDEX_VERSION_COLLECTION = "index_version_collection";    
    
    const std::string FULL_TILE_COLLECTION = "full_tile_collection";
    
    const std::string DATA_DB_SUFFIX = "_data";
    const std::string INDEX_DB_SUFFIX = "_index";

    const int threshold = 20000;
    const int maxTessellationLevel = 2; // possible values are 0, 1, 2
    
    bool index_flag = true;

    //TODO REMOVE FROM HERE... Create a Utility file
    static const std::string base64_chars = 
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                "abcdefghijklmnopqrstuvwxyz"
                "0123456789+/";


    //const std::string reponame = "repoEU";

    static inline bool is_base64(unsigned char c) {
        return (isalnum(c) || (c == '+') || (c == '/'));
    }

    std::string base64_encode(unsigned char const* bytes_to_encode, unsigned int in_len) {
        std::string ret;
        int i = 0;
        int j = 0;
        unsigned char char_array_3[3];
        unsigned char char_array_4[4];

        while (in_len--) {
            char_array_3[i++] = *(bytes_to_encode++);
            if (i == 3) {
                char_array_4[0] = (char_array_3[0] & 0xfc) >> 2;
                char_array_4[1] = ((char_array_3[0] & 0x03) << 4) + ((char_array_3[1] & 0xf0) >> 4);
                char_array_4[2] = ((char_array_3[1] & 0x0f) << 2) + ((char_array_3[2] & 0xc0) >> 6);
                char_array_4[3] = char_array_3[2] & 0x3f;

                for(i = 0; (i <4) ; i++)
                    ret += base64_chars[char_array_4[i]];
                i = 0;
            }
        }

        if (i)
        {
            for(j = i; j < 3; j++)
                char_array_3[j] = '\0';

            char_array_4[0] = ( char_array_3[0] & 0xfc) >> 2;
            char_array_4[1] = ((char_array_3[0] & 0x03) << 4) + ((char_array_3[1] & 0xf0) >> 4);
            char_array_4[2] = ((char_array_3[1] & 0x0f) << 2) + ((char_array_3[2] & 0xc0) >> 6);
            char_array_4[3] =   char_array_3[2] & 0x3f;

            for (j = 0; (j < i + 1); j++)
                ret += base64_chars[char_array_4[j]];

            while((i++ < 3))
                ret += '=';

        }

        return ret;
    }
        
    std::string base64_decode(std::string const& encoded_string) {
        int in_len = encoded_string.size();
        int i = 0;
        int j = 0;
        int in_ = 0;
        unsigned char char_array_4[4], char_array_3[3];
        std::string ret;

        while (in_len-- && ( encoded_string[in_] != '=') && is_base64(encoded_string[in_])) {
            char_array_4[i++] = encoded_string[in_]; in_++;
            if (i ==4) {
                for (i = 0; i <4; i++)
                    char_array_4[i] = base64_chars.find(char_array_4[i]);

                char_array_3[0] = ( char_array_4[0] << 2       ) + ((char_array_4[1] & 0x30) >> 4);
                char_array_3[1] = ((char_array_4[1] & 0xf) << 4) + ((char_array_4[2] & 0x3c) >> 2);
                char_array_3[2] = ((char_array_4[2] & 0x3) << 6) +   char_array_4[3];

                for (i = 0; (i < 3); i++)
                    ret += char_array_3[i];
                i = 0;
            }
        }

        if (i) {
            for (j = i; j <4; j++)
            char_array_4[j] = 0;

            for (j = 0; j <4; j++)
            char_array_4[j] = base64_chars.find(char_array_4[j]);

            char_array_3[0] = (char_array_4[0] << 2) + ((char_array_4[1] & 0x30) >> 4);
            char_array_3[1] = ((char_array_4[1] & 0xf) << 4) + ((char_array_4[2] & 0x3c) >> 2);
            char_array_3[2] = ((char_array_4[2] & 0x3) << 6) + char_array_4[3];

            for (j = 0; (j < i - 1); j++) ret += char_array_3[j];
        }
        return ret;
    }

    const char HEX2DEC[256] = {
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

    std::string uriDecode(const std::string & sSrc) {
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

    std::string calculateLATLONName(double x, double y) {    
        std::stringstream stream_x;
        stream_x << std::setfill('0') << std::setw(3) << std::abs(x);

        std::stringstream stream_y;
        stream_y << std::setfill('0') << std::setw(3) << std::abs(y);

        std::string sign_x = "";
        if (x < 0)
                sign_x = "-";

        std::string sign_y = "";
        if (y < 0)
                sign_y = "-";

        return sign_x+stream_x.str()+"/"+sign_y+stream_y.str();

    }
    
    static double floor10(double input, int precision) {
        return std::floor(input * std::pow(10, precision)) / std::pow(10, precision);
    }

    std::vector<std::string> toStringsForNDNname (double coord) {
//          std::cout<<"in toStringsForNDNname - coord="<<coord<<std::endl;
        
        std::vector<std::string> res(3,"");
        
        double fractpart, intpart;
        fractpart = std::modf (coord , &intpart);
        fractpart = floor10(fractpart+0.0000000001 , 2);
        
//          std::cout << "intpart " << intpart << std::endl;
//          std::cout << "fractpart " << fractpart << std::endl;
        
        res[0] = std::to_string((int)std::abs(intpart));
//          std::cout << "res[0] " << res[0] << std::endl;
        res[1] = std::to_string((int)std::abs(fractpart*10));
//          std::cout << "res[1] " << res[1] << std::endl;
        res[2] = std::to_string((int)std::abs(fractpart*100)-10*(int)std::abs(fractpart*10));
//          std::cout << "res[2] " << res[2] << std::endl;
        return res;
    }
    
    std::string calculateLATLONName(double x, double y, int precision) {
//         std::cout<<"in calculateLATLONName - x="<<x<<" - y="<<y<<std::endl;
        std::vector<std::string> xString = toStringsForNDNname(x);
        std::vector<std::string> yString = toStringsForNDNname(y);
        std::string result = "";
        
        std::stringstream stream_x;
        stream_x<< std::setfill('0') << std::setw(3) <<xString[0];
        std::stringstream stream_y;
        stream_y<< std::setfill('0') << std::setw(3) <<yString[0];
        
        result.append("/");
        result.append((x<0) ? "-" : "");
        result.append(stream_x.str());
        result.append("/");
        result.append((y<0) ? "-" : "");
        result.append(stream_y.str());
        
//          std::cout << "result1 : " << result << std::endl;
        for (int i = 1; i <= precision; i++) {
            result.append("/");
            result.append(xString[i]);
            result.append(yString[i]);
        }
//          std::cout<<"result2 : "<<result<<std::endl;
        return result;
    }

   /* void removeFromParentNode(std::string name, int level) {
        
        //TODO
        int children;
        int counter;
        
        mongocxx::client connection {mongocxx::uri{}};
        auto full_tile_conn = connection[INDEX_DB][FULL_TILE_COLLECTION];
        
        for (int i = level; i<3; i++)
        {
            
        }
    }
    
    void fullTileDBWorker (std::string name1, std::string name10, std::string name100, bool isDeletion) {
        int children;
        int counter;
        
        mongocxx::client connection {mongocxx::uri{}};
        auto full_tile_conn = connection[INDEX_DB][FULL_TILE_COLLECTION];
        
        bsoncxx::stdx::optional<bsoncxx::document::value> tile1Result = full_tile_conn.find_one(bsoncxx::builder::stream::document{} << "name1" << name1 << "level" << 0 << finalize);
        if(tile1Result) { // 1x1 tile already exists
            bsoncxx::document::element element_children = tile1Result->view()["children"];
            bsoncxx::document::element element_counter = tile1Result->view()["counter"];
            if(element_children.type() != bsoncxx::type::k_int32)
                std::cout << "ERROR retrieving tile_children "<<  (int)element_children.type() << std::endl;
            else if(element_counter.type() != bsoncxx::type::k_int32)
                std::cout << "ERROR retrieving tile_counter "<<  (int)element_counter.type() << std::endl;
            else {           
                children = element_children.get_int32();
                counter  = element_counter.get_int32();
                if (!isDeletion)
                    full_tile_conn.update_one(bsoncxx::builder::stream::document{} << "name1" << name1 << "level" << 0 << finalize,                        bsoncxx::builder::stream::document{} << "$set" << bsoncxx::builder::stream::open_document << "children" << ++children << "counter" << ++counter << bsoncxx::builder::stream::close_document << finalize);
                else if (isDeletion && children>1)
                    full_tile_conn.update_one(bsoncxx::builder::stream::document{} << "name1" << name1 << "level" << 0 << finalize,                        bsoncxx::builder::stream::document{} << "$set" << bsoncxx::builder::stream::open_document << "children" << --children << "counter" << --counter << bsoncxx::builder::stream::close_document << finalize);
               
                else if (isDeletion && counter <= 1) {
                    mongocxx::stdx::optional<mongocxx::result::delete_result> result = full_tile_conn.delete_many(bsoncxx::builder::stream::document{} << "name1" << name1 << "level" << 0 << finalize);
                    removeFromParentNode(name1, 2);
                }
        } else {    //new 1x1 tile
            bsoncxx::builder::stream::document shard_document{};
            shard_document <<"name100"<<name100<<"name10"<<name10<< "name1" << name1 << "advertise" << true << "children" << it->second <<"level"<<2 << "counter" << 1 << "cancelled" << false << "leaf" << true;
            auto insert_one = full_tile_conn.insert_one(shard_document.view());
            index_changed = true;
        }

        bsoncxx::stdx::optional<bsoncxx::document::value> tile10Result = full_tile_conn.find_one(bsoncxx::builder::stream::document{} << "name10" << name10 << "level" << 1 << finalize);
        if(tile10Result) 
        {
            //10x10 tile already exists
            bsoncxx::document::element element_children = tile10Result->view()["children"];
            bsoncxx::document::element element_counter = tile10Result->view()["counter"];
            if(element_children.type() != bsoncxx::type::k_int32)
                std::cout << "ERROR retrieving tile_children "<<  (int)element_children.type() << std::endl;
            else if(element_counter.type() != bsoncxx::type::k_int32)
                std::cout << "ERROR retrieving tile_counter "<<  (int)element_counter.type() << std::endl;
            else
            {           
                children = element_children.get_int32();
                counter  = element_counter.get_int32();
    //                         std::cout << "for shard " << shardName <<" children was"  << children << std::endl;
                full_tile_conn.update_one(bsoncxx::builder::stream::document{} << "name10" << name10 << "level" << 1 << finalize,
                    bsoncxx::builder::stream::document{} << "$set" << bsoncxx::builder::stream::open_document << "children" << ++children << "counter" << ++counter << bsoncxx::builder::stream::close_document << finalize);
                
            }
        }
        else {  //new 10x10 tile
            bsoncxx::builder::stream::document shard_document{};
            shard_document <<"name100"<<name100<<"name10"<<name10<< "name1" << "xx" << "advertise" << true << "children" << 1 <<"level"<< 1 << "counter" << 1 << "cancelled" << false << "leaf" << false;
            auto insert_one = full_tile_conn.insert_one(shard_document.view());
            //new10=true;
        }
        
        bsoncxx::stdx::optional<bsoncxx::document::value> tile100Result = full_tile_conn.find_one(bsoncxx::builder::stream::document{} << "name100" << name100 << "level" << 0 << finalize);
        if(tile100Result) 
        {
            bsoncxx::document::element element_counter = tile100Result->view()["counter"];
            bsoncxx::document::element element = tile100Result->view()["children"];
            if(element.type() != bsoncxx::type::k_int32)
                std::cout << "ERROR retrieving tile_children "<<  (int)element.type() << std::endl;
            else
            {           
                children = element.get_int32();
                counter  = element_counter.get_int32();
    //                         std::cout << "for shard " << shardName <<" children was"  << children << std::endl;
                full_tile_conn.update_one(bsoncxx::builder::stream::document{} << "name100" << name100 << "level" << 0 << finalize,
                    bsoncxx::builder::stream::document{} << "$set" << bsoncxx::builder::stream::open_document << "children" << ++children << "counter" << ++counter << bsoncxx::builder::stream::close_document << finalize);
            }
        }
        else {
            bsoncxx::builder::stream::document shard_document{};
            shard_document <<"name100"<<name100<<"name10"<<"xx"<< "name1" << "xx" << "advertise" << true << "children" << 1 <<"level"<<0 << "counter" << 1 << "cancelled" << false << "leaf" << false;
            auto insert_one = full_tile_conn.insert_one(shard_document.view());
        }
    }
    }
    */
   
   /*bool FullTileCollectionUpdater (std::list<std::pair<double,double> >coordSet, bool isDeletion) {
        
        std::string name100;
        std::string name10; 
        std::string name1;
        
        std::list< std::pair<double,double> >::iterator it;
        for(it=coordSet.begin(); it!=coordSet.end(); ++it)
        {
            double x = it->first;
            double y = it->second;
        
            name1 = calculateLATLONName(x,y,2);
                        
            std::vector<std::string> componentNameVector;
            if (boost::starts_with(name1, "/")==false)
                name1 = "/" + name1;
            boost::split(componentNameVector, name1, boost::is_any_of("/"));
            
            if (componentNameVector.size() != 5) {   // 1x1 tile name, 5 components
                std::cout << "ERROR: processing tile with name.size()=" <<  componentNameVector.size() << " (instead of 5)"<< std::endl;
                return;
            } else {
                name100 = "/"+componentNameVector[1]+"/"+componentNameVector[2];
                name10  = "/"+componentNameVector[1]+"/"+componentNameVector[2]+"/"+componentNameVector[3];
                
                fullTileDBWorker (name1, name10, name100, isDeletion)
            }
        }
    } */
    
    
   
    void constrainedTesselation() {
        
        mongocxx::client connection {mongocxx::uri{}};
        auto full_tile_conn = connection[INDEX_DB][FULL_TILE_COLLECTION];
        
        int nLeaf=full_tile_conn.count(bsoncxx::builder::stream::document{} << "leaf"<< true <<finalize);
        
//         std::cout<<"nLeaf="<<nLeaf<<std::endl;
        
        int i,x,y=0;
        bool agg_needed;
        std::string name_key="";
        std::cout<<"Starting Tassellation"<<std::endl;
        std::cout<<"current nLeaf="<<nLeaf<<std::endl;
        while (nLeaf > threshold) {
            i=0;
            while (i<maxTessellationLevel) {
                std::cout<<"Analyzing level "<<i<<" nLeaf="<<nLeaf<<std::endl;
                if(i==0)
                    name_key="name100";
                else if (i==1)
                    name_key="name10";
                // y number of leaf of level less or equal to i (not cancelled by the tree)
                // x number of level i+1 (not cancelled by the tree) 
                //std::cout<<"compute y"<<std::endl;
                y=full_tile_conn.count(bsoncxx::builder::stream::document{} << "level"<< bsoncxx::builder::stream::open_document << "$lte" << i << bsoncxx::builder::stream::close_document << "leaf" << true << "cancelled" << false << finalize);
                //std::cout<<"compute y end:"<<y<<std::endl;
                
                //std::cout<<"compute x"<<std::endl;
                x=full_tile_conn.count(bsoncxx::builder::stream::document{} << "level" << i+1 << "cancelled" << false << finalize);
                //std::cout<<"compute x end:"<<x<<std::endl;
//                 std::cout<< "x " << x << " at level "<< i <<std::endl;
//                 std::cout<< "y " << y << " at level "<< i <<std::endl;
                
                agg_needed = (x+y)>threshold;
                if (agg_needed) {
                    // find tile with max counter (min tile stretch)
                    
                    /*
                    bsoncxx::builder::stream::document group_document{};
                    group_document << "_id" << "$"+name_key << "maxCounter" <<  bsoncxx::builder::stream::open_document << "$max" << "$counter" << bsoncxx::builder::stream::close_document ;
                    mongocxx::pipeline maxCounter_pipeline;
                   
                    bsoncxx::builder::stream::document match_document{};
                    match_document << "level" << i << "cancelled" << false << "leaf" << false;
                    maxCounter_pipeline.match(match_document.view());
                    maxCounter_pipeline.group(group_document.view());
                    
//                     std::cout<<bsoncxx::to_json(match_document.view())<<std::endl;
//                     std::cout<<bsoncxx::to_json(group_document.view())<<std::endl;
                    
                    mongocxx::cursor max_cursor = full_tile_conn.aggregate(maxCounter_pipeline);*/
                    
                    mongocxx::options::find opts{};
                    opts.projection(bsoncxx::builder::stream::document{} << name_key << 1 << "counter" << 1 <<  "children" << 1 << finalize);
                    opts.sort(bsoncxx::builder::stream::document{}<< "counter" << -1 << finalize);
                    //opts.limit(1);
                    //std::cout << "search aggregable tile " << std::endl;
                    
                    mongocxx::cursor max_cursor = full_tile_conn.find(bsoncxx::builder::stream::document{}<<"level" << i << "cancelled" << false << "leaf" << false << finalize, opts);
                    //std::cout<<"search aggregable tile end"<<std::endl;
                    
                    for(auto doc : max_cursor) {
                        //std::cout<<"tile aggregation"<<std::endl;
                        bsoncxx::document::element name_element = doc[name_key];//doc["_id"];
//                         bsoncxx::document::element counter_element = doc["counter"];//doc["_id"];
                        bsoncxx::document::element children_element = doc["children"];//doc["_id"];
                        
                        
                        int children=children_element.get_int32();
                        std::string name = name_element.get_utf8().value.to_string();
                        //std::cout<< "name: "<< name <<" children: "<<children<< "counter :"<<counter_element.get_int32()<<std::endl;
//                         std::cout<< "found element with name " << name<< " at level "<<i<<std::endl;
                        
                        if(name=="xx"){
                            std::cout<< "found element with name " << name<< " at level "<<i<<std::endl;
                            exit(EXIT_FAILURE);
                        }
                        //set the flag leaf = true on the selected sorted tile 
                        full_tile_conn.update_one(bsoncxx::builder::stream::document{} << name_key << name << "level" << i << "cancelled" << false << finalize,
                        bsoncxx::builder::stream::document{} << "$set" << bsoncxx::builder::stream::open_document << "leaf" << true << bsoncxx::builder::stream::close_document << finalize);
                        
                        //set cancelled=true to all children
                        full_tile_conn.update_many(bsoncxx::builder::stream::document{} << name_key << name << "level" << bsoncxx::builder::stream::open_document << "$gt" << i << bsoncxx::builder::stream::close_document << finalize,
                        bsoncxx::builder::stream::document{} << "$set" << bsoncxx::builder::stream::open_document << "leaf" << false << "cancelled" << true << bsoncxx::builder::stream::close_document << finalize);
                        
                        //nLeaf=full_tile_conn.count(bsoncxx::builder::stream::document{} << "leaf"<< true <<finalize);
                        //std::cout<<"nLeaf="<<nLeaf<<std::endl;
                        //update x and y
                        x=x-children;
                        y=y+1;
                        if ((x+y)<=threshold) break;
                    }
                    //break;
                }
                i=i+1;
                nLeaf=full_tile_conn.count(bsoncxx::builder::stream::document{} << "leaf"<< true <<finalize);
            }
        }
        double overcovering =
        (1.0*full_tile_conn.count(bsoncxx::builder::stream::document{} << "leaf"<< true << "level" <<0 << finalize)*10000
        + full_tile_conn.count(bsoncxx::builder::stream::document{} << "leaf"<< true << "level" <<1 << finalize)*100 + 
        full_tile_conn.count(bsoncxx::builder::stream::document{} << "leaf"<< true << "level" <<2 << finalize)) / full_tile_conn.count(bsoncxx::builder::stream::document{} << "level" <<2 << finalize);
        std::cout<<"Stopping Tassellation "<<nLeaf<<std::endl;
        std::cout<<"nLeaf = "<<nLeaf<<std::endl;
        std::cout<<"overcovering = "<<overcovering<<std::endl;
        
    }
    
    void initializeFullTileDB() {
        
        mongocxx::client connection {mongocxx::uri{}};

        auto full_tile_conn = connection[INDEX_DB][FULL_TILE_COLLECTION];
        full_tile_conn.drop();
                
        bsoncxx::builder::stream::document index_builder_100;
        index_builder_100 << "name100" << 1;
        full_tile_conn.create_index(index_builder_100.view(), {});
        
        bsoncxx::builder::stream::document index_builder_10;
        index_builder_10 << "name10" << 1;
        full_tile_conn.create_index(index_builder_10.view(), {});
        
        bsoncxx::builder::stream::document index_builder_counter;
        index_builder_counter << "counter" << 1;
        full_tile_conn.create_index(index_builder_counter.view(), {});
        
        bsoncxx::builder::stream::document index_builder_level;
        index_builder_level << "level" << 1;
        full_tile_conn.create_index(index_builder_level.view(), {});
        
        bsoncxx::builder::stream::document index_builder_leaf;
        index_builder_leaf << "leaf" << 1;
        full_tile_conn.create_index(index_builder_leaf.view(), {});
        
        bsoncxx::builder::stream::document index_builder_cancelled;
        index_builder_cancelled << "cancelled" << 1;
        full_tile_conn.create_index(index_builder_cancelled.view(), {});
    
        
    }
    
    void MongoStorage::buildIndexDB() {
        mongocxx::client connection {mongocxx::uri{}};
        // build index_db to share
        auto index_db_conn = connection[INDEX_DB][INDEX_COLLECTION];
        auto full_tile_conn = connection[INDEX_DB][FULL_TILE_COLLECTION];
        index_db_conn.drop();
        
        mongocxx::cursor cursor = full_tile_conn.find(bsoncxx::builder::stream::document{} << "leaf" << true << finalize);  
        for(auto doc : cursor) {
            int level = doc["level"].get_int32();
            std::string name_key;
            if (level==0) {name_key="name100";}
            else if (level==1) {name_key="name10";}
            else name_key="name1";    
            std::string name = doc[name_key].get_utf8().value.to_string();
            
            bsoncxx::builder::stream::document document{};
            document << "name" << name << "counter" << 1000;
            auto insert_one = index_db_conn.insert_one(document.view());
        }
        
        //update index version
        versionUpdater();
        
    }
    
    void MongoStorage::FullTileCollectionRestore(std::map<std::string,std::pair<int,int>> coordMap1,std::map<std::string,std::pair<int,int>> coordMap10,std::map<std::string,std::pair<int,int>> coordMap100) {
        
      
        std::cout << "FullTileCollectionRestore started" << std::endl; 
        mongocxx::client connection {mongocxx::uri{}};
        auto full_tile_conn = connection[INDEX_DB][FULL_TILE_COLLECTION];
        
        initializeFullTileDB(); // create db and indexes

        std::map<std::string, std::pair<int,int>>::iterator it;
        
        int children;
        int counter;
        std::string tileName;
        std::string name100;
        std::string name10;
        std::string name1;
        for (it=coordMap1.begin(); it!=coordMap1.end(); ++it)
        {
            // insert 1x1
            tileName = it->first;
            children = it->second.second;
            counter = it->second.first;
            name1= tileName;                      
            std::vector<std::string> componentNameVector;
            if (boost::starts_with(tileName, "/")==false)
                tileName = "/" + tileName;
            boost::split(componentNameVector, tileName, boost::is_any_of("/"));
            name100 = "/"+componentNameVector[1]+"/"+componentNameVector[2];
            name10  = "/"+componentNameVector[1]+"/"+componentNameVector[2]+"/"+componentNameVector[3];
            bsoncxx::builder::stream::document shard_document{};
            shard_document <<"name100"<<name100<<"name10"<<name10<< "name1" << name1 << "advertise" << true << "children" << children <<"level" << 2 << "counter" << counter << "cancelled" << false << "leaf" << true;
            auto insert_one = full_tile_conn.insert_one(shard_document.view());
        }
        
        for (it=coordMap10.begin(); it!=coordMap10.end(); ++it)
        {
            // insert 10x10
            tileName = it->first;
            children = it->second.second;
            counter = it->second.first;
            name10=tileName;
            name1= "xx";                      
            std::vector<std::string> componentNameVector;
            if (boost::starts_with(tileName, "/")==false)
                tileName = "/" + tileName;
            boost::split(componentNameVector, tileName, boost::is_any_of("/"));
            name100 = "/"+componentNameVector[1]+"/"+componentNameVector[2];
            bsoncxx::builder::stream::document shard_document{};
            shard_document <<"name100"<<name100<<"name10"<<name10<< "name1" << name1 << "advertise" << true << "children" << children <<"level"<<1 << "counter" << counter << "cancelled" << false << "leaf" << false; //TODO leaf false??
            auto insert_one = full_tile_conn.insert_one(shard_document.view());
        }
        for (it=coordMap100.begin(); it!=coordMap100.end(); ++it)
        {
            // insert 100x100
            tileName = it->first;
            children = it->second.second;
            counter = it->second.first;
            name100=tileName;
            name1= "xx";
            name10="xx";
            std::vector<std::string> componentNameVector;
            if (boost::starts_with(tileName, "/")==false)
                tileName = "/" + tileName;
            boost::split(componentNameVector, tileName, boost::is_any_of("/"));
            bsoncxx::builder::stream::document shard_document{};
            shard_document <<"name100"<<name100<<"name10"<<name10<< "name1" << name1 << "advertise" << true << "children" << children <<"level"<<0 << "counter" << counter << "cancelled" << false << "leaf" << false; //TODO leaf false??
            auto insert_one = full_tile_conn.insert_one(shard_document.view());
        }
        std::cout << "FullTileCollectionRestore finished" << std::endl; 
      
        
        resetFullTileDB();
        
        constrainedTesselation();

        buildIndexDB();
        
    }
    
    /*void FullTileCollectionRestore(std::map<std::string,std::pair<int,int>> coordMap) {
        
        return;
        std::cout << "FullTileCollectionRestore started" << std::endl; 
        mongocxx::client connection {mongocxx::uri{}};
        auto full_tile_conn = connection[INDEX_DB][FULL_TILE_COLLECTION];
        
        initializeFullTileDB(); // create db and indexes

        std::map<std::string, std::pair<int,int>>::iterator it;
        
        int children;
        int counter;
        bool new1;
        bool new10;
        std::string name100;
        std::string name10; 
        std::string tileName;
        
        for (it=coordMap.begin(); it!=coordMap.end(); ++it)
        {
            
            tileName = it->first;
            new1 = true;
            new10=false;
                        
            std::vector<std::string> componentNameVector;
            if (boost::starts_with(tileName, "/")==false)
                tileName = "/" + tileName;
            boost::split(componentNameVector, tileName, boost::is_any_of("/"));
            if (componentNameVector.size() == 5) {   // 1x1 tile name, 5 components
                name100 = "/"+componentNameVector[1]+"/"+componentNameVector[2];
                name10  = "/"+componentNameVector[1]+"/"+componentNameVector[2]+"/"+componentNameVector[3];
                
                // insert 1x1
                bsoncxx::builder::stream::document shard_document{};
                shard_document <<"name100"<<name100<<"name10"<<name10<< "name1" << it->first << "advertise" << true << "children" << it->second <<"level"<<2 << "counter" << 1 << "cancelled" << false << "leaf" << true;
                auto insert_one = full_tile_conn.insert_one(shard_document.view());
                // insert 10x10
                bsoncxx::stdx::optional<bsoncxx::document::value> tile10Result = full_tile_conn.find_one(bsoncxx::builder::stream::document{} << "name10" << name10 << "level" << 1 << finalize);
                if(tile10Result) 
                {
                    
                }
                // insert 100x100
                
            } else {
                std::cout << "ERROR: processing tile with name.size()=" <<  componentNameVector.size() << " (instead of 5)"<< std::endl;
                return;
            }
            
            if (new1) {
                bsoncxx::stdx::optional<bsoncxx::document::value> tile10Result = full_tile_conn.find_one(bsoncxx::builder::stream::document{} << "name10" << name10 << "level" << 1 << finalize);
                if(tile10Result) 
                {
                    //10x10 tile already exists
                    bsoncxx::document::element element_children = tile10Result->view()["children"];
                    bsoncxx::document::element element_counter = tile10Result->view()["counter"];
                    if(element_children.type() != bsoncxx::type::k_int32)
                        std::cout << "ERROR retrieving tile_children "<<  (int)element_children.type() << std::endl;
                    else if(element_counter.type() != bsoncxx::type::k_int32)
                        std::cout << "ERROR retrieving tile_counter "<<  (int)element_counter.type() << std::endl;
                    else
                    {           
                        children = element_children.get_int32();
                        counter  = element_counter.get_int32();
//                         std::cout << "for shard " << shardName <<" children was"  << children << std::endl;
                        full_tile_conn.update_one(bsoncxx::builder::stream::document{} << "name10" << name10 << "level" << 1 << finalize,
                            bsoncxx::builder::stream::document{} << "$set" << bsoncxx::builder::stream::open_document << "children" << ++children << "counter" << ++counter << bsoncxx::builder::stream::close_document << finalize);
                        
                    }
                }
                else {  //new 10x10 tile
                    bsoncxx::builder::stream::document shard_document{};
                    shard_document <<"name100"<<name100<<"name10"<<name10<< "name1" << "xx" << "advertise" << true << "children" << 1 <<"level"<< 1 << "counter" << 1 << "cancelled" << false << "leaf" << false;
                    auto insert_one = full_tile_conn.insert_one(shard_document.view());
                    new10=true;
                }
            }
            if (new1) {
                bsoncxx::stdx::optional<bsoncxx::document::value> tile100Result = full_tile_conn.find_one(bsoncxx::builder::stream::document{} << "name100" << name100 << "level" << 0 << finalize);
                if(tile100Result) 
                {
                    bsoncxx::document::element element_counter = tile100Result->view()["counter"];
                    bsoncxx::document::element element = tile100Result->view()["children"];
                    if(element.type() != bsoncxx::type::k_int32)
                        std::cout << "ERROR retrieving tile_children "<<  (int)element.type() << std::endl;
                    else
                    {           
                        children = element.get_int32();
                        counter  = element_counter.get_int32();
//                         std::cout << "for shard " << shardName <<" children was"  << children << std::endl;
                        int cinc=0;
                        if (new10) cinc=1;
                        full_tile_conn.update_one(bsoncxx::builder::stream::document{} << "name100" << name100 << "level" << 0 << finalize,
                            bsoncxx::builder::stream::document{} << "$set" << bsoncxx::builder::stream::open_document << "children" << children+cinc << "counter" << ++counter << bsoncxx::builder::stream::close_document << finalize);
                    }
                }
                else {
                    bsoncxx::builder::stream::document shard_document{};
                    shard_document <<"name100"<<name100<<"name10"<<"xx"<< "name1" << "xx" << "advertise" << true << "children" << 1 <<"level"<<0 << "counter" << 1 << "cancelled" << false << "leaf" << false;
                    auto insert_one = full_tile_conn.insert_one(shard_document.view());
                }
            }
        }
      
        constrainedTesselation();

        buildIndexDB();
        
    }
    */
    
    void MongoStorage::resetFullTileDB() {
        // level is the lowest level of tessellation
        // This function set cancelled false to all the full tile db and set leaf true only on 1x1
        std::cout<<"Starting resetFullTileDB"<<std::endl;
        
        mongocxx::client connection {mongocxx::uri{}};

        auto full_tile_conn = connection[INDEX_DB][FULL_TILE_COLLECTION];
        
        //set cancelled=false and leaf=false to all
        
        full_tile_conn.update_many(bsoncxx::builder::stream::document{} << finalize,
        bsoncxx::builder::stream::document{} << "$set" << bsoncxx::builder::stream::open_document << "leaf" << false << "cancelled" << false << bsoncxx::builder::stream::close_document << finalize);
        
        //set cancelled=false and leaf=true to level
        full_tile_conn.update_many(bsoncxx::builder::stream::document{} <<"level"<<maxTessellationLevel<< finalize,
        bsoncxx::builder::stream::document{} << "$set" << bsoncxx::builder::stream::open_document << "leaf" << true << bsoncxx::builder::stream::close_document << finalize);
        
        return;
    
        
    }

    MongoStorage::~MongoStorage(){}

    MongoStorage::MongoStorage(const string& dbPath,Name dataprefix): m_size(0) {
        if (dbPath.empty()) {
            std::cerr << "Create db file in local location [" << dbPath << "]. " << std::endl
                        << "You can assign the path using -d option" << std::endl;
            m_dbPath = string("ndn_repo.db");
        }
        else {
            boost::filesystem::path fsPath(dbPath);
            boost::filesystem::file_status fsPathStatus = boost::filesystem::status(fsPath);
            if (!boost::filesystem::is_directory(fsPathStatus)) {
                if (!boost::filesystem::create_directory(boost::filesystem::path(fsPath))) {
                    BOOST_THROW_EXCEPTION(Error("Folder '" + dbPath + "' does not exists and cannot be created"));
                }
            }
            m_dbPath = dbPath + "/ndn_repo.db";
        }
        initializeRepo();
        reponame=dataprefix.get(dataprefix.size()-1).toUri();
        std::cout<<"Repo Name MongoStorage"<<reponame<<std::endl;
    }

    void MongoStorage::initializeRepo() {
        std::thread versionThread(&MongoStorage::sendVersion, this);
        versionThread.detach();

        std::thread versionCheckThread(&MongoStorage::checkVersion, this);
        versionCheckThread.detach();
    }

    void MongoStorage::sendVersion() {

        sleep(2);
        const int ADVERTISING_DELAY = 60;

        mongocxx::client connection{mongocxx::uri{}};   
        auto data_connection = connection[INDEX_DB][INDEX_VERSION_COLLECTION];

        while (index_flag == true)
        {
            bsoncxx::stdx::optional<bsoncxx::document::value> version_document_result = data_connection.find_one(bsoncxx::builder::stream::document{} << "name" << "INDEX_VERSION" << finalize);
            if(version_document_result) 
            {
                bsoncxx::document::element element = version_document_result->view()["index_version"];

                if(element.type() != bsoncxx::type::k_int32) {
                    std::cout << "ERROR retrieving index_version "<<  (int)element.type() << std::endl;
                }
                else
                {
                    int index_version_value = element.get_int32();
//                     std::cout << "sending interest: index_version = "<< index_version_value << std::endl;
                    SendVersionInterest(index_version_value);
                }
            }
            sleep(ADVERTISING_DELAY);
        } 
    }

    void MongoStorage::checkVersion() {
        
        std::cout<<"Starting checkVersion"<<std::endl;
        resetFullTileDB();
        constrainedTesselation();
        buildIndexDB();
        
        //return;
        while (index_flag == true) {
            
            std::cout << "checkVersion started" << std::endl;
            struct timeval tp;
            gettimeofday(&tp, NULL);
            long int start = tp.tv_sec * 1000 + tp.tv_usec / 1000; //get current timestamp in milliseconds

            const int CHECK_VERSION_DELAY = 6*60*60;   //6 hours

            if (true) {
                mongocxx::client connection {mongocxx::uri{}};
                std::map<std::string, std::pair<int,int>> coordMap1 = std::map<std::string, std::pair<int,int>>(); // tile name, active children, active geoJSON element inside 
                std::map<std::string, std::pair<int,int>> coordMap10 = std::map<std::string, std::pair<int,int>>(); // tile name, active children, active tile 1x1 inside
                std::map<std::string, std::pair<int,int>> coordMap100 = std::map<std::string, std::pair<int,int>>(); // tile name, active children, active tile 1x1 inside

                for(auto db : connection.list_databases()) {
                    std::string db_name =  db["name"].get_utf8().value.to_string();

                    if (db_name.find(INDEX_DB_SUFFIX) != std::string::npos) {
                        //std::cout << db_name <<" found!" << '\n';

                        mongocxx::database database = connection[db_name];

                        for(auto coll : database.list_collections()) {
                            std::string collection_name =  coll["name"].get_utf8().value.to_string();
                            //std::cout << "\t" <<collection_name << "\n";

                            mongocxx::collection collection = database[collection_name];
                            mongocxx::cursor cursor = collection.find(bsoncxx::builder::stream::document{} << finalize);
                            for(auto doc : cursor) {
                                std::stringstream result_stream;
                                result_stream << bsoncxx::to_json(doc);
                                
                                rapidjson::Document json_data;
                                json_data.Parse(const_cast<char*> (result_stream.str().c_str()));
                                
                                if (json_data.HasMember("content")) {   
                                    const Value& geoJSON = json_data["content"];
                                    rapidjson::Document geoJSON_doc;
                                    geoJSON_doc.CopyFrom(geoJSON, geoJSON_doc.GetAllocator());
                                    for(std::pair<double,double> coord : calculateGeometryIndex(geoJSON_doc)) {
                                        bool new1=false;
                                        bool new10=false;
                                        
                                        std::string coordName1 = calculateLATLONName(coord.first, coord.second, 2);
                                        std::map<string, std::pair<int,int>>::iterator it = coordMap1.find(coordName1); 
                                        
                                        if (it != coordMap1.end())
                                            it->second.first++; // increase the object geoJSON counter (children always 0 for 1x1)
                                        else {
                                            coordMap1.insert(std::pair<std::string,std::pair<int,int>>(coordName1, std::pair<int,int>(1,1)));
                                            new1=true;
                                        }
                                        
                                        std::vector<std::string> coordNameVector;
                                        boost::split(coordNameVector, coordName1, boost::is_any_of("/"));
                                        std::string coordName100 = "/"+coordNameVector[1]+"/"+coordNameVector[2];
                                        std::string coordName10 = "/"+coordNameVector[1]+"/"+coordNameVector[2]+"/"+coordNameVector[3];
                                        //std::cout << "coordName100: "<< coordName100 <<" coordName10 " <<coordName10<<" coordName1 " <<coordName1<<std::endl;
                                        
                                        // update tile 10x10
                                        it = coordMap10.find(coordName10); 
                                        
                                        if (it != coordMap10.end()) {
                                            if (new1) it->second.first++; // increase the 1x1 tile counter
                                            if (new1) it->second.second++; // increase children counter 
                                        } else {
                                            coordMap10.insert(std::pair<std::string,std::pair<int,int>>(coordName10, std::pair<int,int>(1,1)));
                                            new10=true;
                                        }
                                        
                                        // update tile 100x100
                                        it = coordMap100.find(coordName100); 
                                        
                                        if (it != coordMap100.end()) {
                                            if (new1) it->second.first++; // increase the 1x1 tile counter
                                            if (new10) it->second.second++; // increase children counter
                                        } else {
                                            coordMap100.insert(std::pair<std::string,std::pair<int,int>>(coordName100, std::pair<int,int>(1,1)));
                                        }
                                        
                                    }

                                }
                            }
                        }    
                    }
                }
                
                //gettimeofday(&tp, NULL);
                // long int time1 = tp.tv_sec * 1000 + tp.tv_usec / 1000; //get current timestamp in milliseconds
        
                /*    
                std::map<std::string, int>::iterator it;
                for (it=coordMap.begin(); it!=coordMap.end(); ++it)
                    std::cout << it->first << " => " << it->second << '\n';
                

                //RETRIEVE ACTUAL LIST!! 
                auto index_version_collection = connection[INDEX_DB][INDEX_COLLECTION];
                mongocxx::options::find opts{};
                opts.projection(bsoncxx::builder::stream::document{} << "name" << 1 <<"counter" << 1 <<  "_id" << 0 << finalize);
                auto cursor = index_version_collection.find(bsoncxx::builder::stream::document{} << finalize, opts);
                
                std::map<std::string, int> oldMap = std::map<std::string, int>();
                
                for (auto&& doc : cursor) {    
                    bsoncxx::document::element counter_element = doc["counter"];
                    bsoncxx::document::element name_element = doc["name"];
                    if(counter_element.type() == bsoncxx::type::k_int32 && name_element.type() == bsoncxx::type::k_utf8) {
                        int counter = counter_element.get_int32();
                        std::string name = name_element.get_utf8().value.to_string();                    
                        oldMap.insert ( std::pair<std::string,int>(name, counter) );
                    }        
                }
    //             long int time2 = tp.tv_sec * 1000 + tp.tv_usec / 1000; //get current timestamp in milliseconds
                if (coordMap != oldMap) {
    //                 std::cout << "INDEX DB out of sync!!! " << std::endl;
                    index_version_collection.drop();

                    std::map<std::string, int>::iterator it;
                    for (it=coordMap.begin(); it!=coordMap.end(); ++it) {
    //                     std::cout << it->first << " => " << it->second << '\n';
                        bsoncxx::builder::stream::document shard_document{};
                        shard_document << "name" << it->first << "counter" << it->second;
                        auto insert_one = index_version_collection.insert_one(shard_document.view());
    //                     bsoncxx::oid oid = insert_one->inserted_id().get_oid().value;
    //                     std::string JobID = oid.to_string();
    //                     std::cout << "insert_one: " << JobID << std::endl;
                    }
                    std::cout << "INDEX DB RESTORE DONE " << std::endl;
                    versionUpdater();
                }
                
                */
                
                FullTileCollectionRestore(coordMap1,coordMap10,coordMap100);
            }
    
            gettimeofday(&tp, NULL);
            long int stop = tp.tv_sec * 1000 + tp.tv_usec / 1000; //get current timestamp in milliseconds
            std::cout << "CKECK_VERSION Time: " << stop-start <<" ms" <<std::endl;  
//             std::cout << "Time1 : " << time1-start <<" ms" <<std::endl;
//             std::cout << "Time2 : " << time2-time1 <<" ms" <<std::endl;

            sleep(CHECK_VERSION_DELAY);
        }

    }
    
    /*void MongoStorage::checkVersionOld() {
        
        
        while (index_flag == true) {
            
            std::cout << "checkVersion started" << std::endl;
            struct timeval tp;
            gettimeofday(&tp, NULL);
            long int start = tp.tv_sec * 1000 + tp.tv_usec / 1000; //get current timestamp in milliseconds

            const int CHECK_VERSION_DELAY = 6*60*60;   //6 hours

            //resetFullTileDB();
            //constrainedTesselation();
            //buildIndexDB();
            // debug Purposes
            if (true) {
            mongocxx::client connection {mongocxx::uri{}};
            std::map<std::string, int> coordMap = std::map<std::string, int>();

            for(auto db : connection.list_databases()) {
                std::string db_name =  db["name"].get_utf8().value.to_string();

                if (db_name.find(INDEX_DB_SUFFIX) != std::string::npos) {
                    //std::cout << db_name <<" found!" << '\n';

                    mongocxx::database database = connection[db_name];

                    for(auto coll : database.list_collections()) {
                        std::string collection_name =  coll["name"].get_utf8().value.to_string();
                        //std::cout << "\t" <<collection_name << "\n";

                        mongocxx::collection collection = database[collection_name];
                        mongocxx::cursor cursor = collection.find(bsoncxx::builder::stream::document{} << finalize);
                        for(auto doc : cursor) {
                            std::stringstream result_stream;
                            result_stream << bsoncxx::to_json(doc);
                            
                            rapidjson::Document json_data;
                            json_data.Parse(const_cast<char*> (result_stream.str().c_str()));
                            
                            if (json_data.HasMember("content")) {   
                                const Value& geoJSON = json_data["content"];
                                rapidjson::Document geoJSON_doc;
                                geoJSON_doc.CopyFrom(geoJSON, geoJSON_doc.GetAllocator());
                                for(std::pair<double,double> coord : calculateGeometryIndex(geoJSON_doc)) {
                                                        //std::string coordName = calculateLATLONName(coord.first, coord.second);
                                    std::string coordName = calculateLATLONName(coord.first, coord.second, 2);
                                    std::map<string, int>::iterator it = coordMap.find(coordName); 
                                    if (it != coordMap.end())
                                        it->second++;
                                    else
                                        coordMap.insert(std::pair<std::string,int>(coordName, 1));
                                }

                            }
                        }
                    }    
                }
            }
            
            //gettimeofday(&tp, NULL);
            // long int time1 = tp.tv_sec * 1000 + tp.tv_usec / 1000; //get current timestamp in milliseconds
    
                
//             std::map<std::string, int>::iterator it;
//             for (it=coordMap.begin(); it!=coordMap.end(); ++it)
//                 std::cout << it->first << " => " << it->second << '\n';
//             
// 
//             //RETRIEVE ACTUAL LIST!! 
//             auto index_version_collection = connection[INDEX_DB][INDEX_COLLECTION];
//             mongocxx::options::find opts{};
//             opts.projection(bsoncxx::builder::stream::document{} << "name" << 1 <<"counter" << 1 <<  "_id" << 0 << finalize);
//             auto cursor = index_version_collection.find(bsoncxx::builder::stream::document{} << finalize, opts);
//             
//             std::map<std::string, int> oldMap = std::map<std::string, int>();
//             
//             for (auto&& doc : cursor) {    
//                 bsoncxx::document::element counter_element = doc["counter"];
//                 bsoncxx::document::element name_element = doc["name"];
//                 if(counter_element.type() == bsoncxx::type::k_int32 && name_element.type() == bsoncxx::type::k_utf8) {
//                     int counter = counter_element.get_int32();
//                     std::string name = name_element.get_utf8().value.to_string();                    
//                     oldMap.insert ( std::pair<std::string,int>(name, counter) );
//                 }        
//             }
// //             long int time2 = tp.tv_sec * 1000 + tp.tv_usec / 1000; //get current timestamp in milliseconds
//             if (coordMap != oldMap) {
// //                 std::cout << "INDEX DB out of sync!!! " << std::endl;
//                 index_version_collection.drop();
// 
//                 std::map<std::string, int>::iterator it;
//                 for (it=coordMap.begin(); it!=coordMap.end(); ++it) {
// //                     std::cout << it->first << " => " << it->second << '\n';
//                     bsoncxx::builder::stream::document shard_document{};
//                     shard_document << "name" << it->first << "counter" << it->second;
//                     auto insert_one = index_version_collection.insert_one(shard_document.view());
// //                     bsoncxx::oid oid = insert_one->inserted_id().get_oid().value;
// //                     std::string JobID = oid.to_string();
// //                     std::cout << "insert_one: " << JobID << std::endl;
//                 }
//                 std::cout << "INDEX DB RESTORE DONE " << std::endl;
//                 versionUpdater();
//             }
            
            
            
            FullTileCollectionRestore(coordMap);
            }
    
            gettimeofday(&tp, NULL);
            long int stop = tp.tv_sec * 1000 + tp.tv_usec / 1000; //get current timestamp in milliseconds
            std::cout << "CKECK_VERSION Time: " << stop-start <<" ms" <<std::endl;  
//             std::cout << "Time1 : " << time1-start <<" ms" <<std::endl;
//             std::cout << "Time2 : " << time2-time1 <<" ms" <<std::endl;

            sleep(CHECK_VERSION_DELAY);
        }

    }
    */
    
    void MongoStorage::SendVersionInterest(int index_version_value) {
        Interest interest(Name("/FES/"+reponame+"/version" + std::to_string(index_version_value)));
        interest.setInterestLifetime(ndn::time::milliseconds(200));
        interest.setMustBeFresh(true);

        m_face.expressInterest(interest,
                            bind(&MongoStorage::onData, this,  _1, _2),
                            bind(&MongoStorage::onTimeout, this, _1));

        std::cout << "Sending " << interest << std::endl;

        // processEvents will block until the requested data received or timeout occurs
        m_face.processEvents();
    }
    
    void MongoStorage::onData(const Interest& interest, const Data& data) {
        std::cout << data << std::endl;
    }

    void MongoStorage::onTimeout(const Interest& interest) {
        std::cout << "Timeout " << interest << std::endl;
    }
    
    void MongoStorage::fullEnumerate(const std::function<void(const Storage::ItemMeta)>& f) {    }

    void printData(Data data) {
        std::string content = "";  
        for (u_int i =0; i< data.getContent().value_size(); i++)
            content.push_back(data.getContent().value()[i]);

        //std::cout << "Content of data: " << data.getName().toUri() << std::endl;
        //std::cout << content << std::endl;
    }

    std::string MongoStorage::calculateDataIdentifier(Name name) {
        std::vector<std::string> componentNameVector;
        std::string name_str = name.toUri();
        boost::split(componentNameVector, name_str, boost::is_any_of("/"));
        std::string data_id = "";
        if (componentNameVector.size() > 3)
            data_id  = componentNameVector.at(3);
        return data_id;
    }

    std::string MongoStorage::calculateTenantIdentifier(Name name) {
        std::vector<std::string> componentNameVector;
        std::string name_str = name.toUri();
        boost::split(componentNameVector, name_str, boost::is_any_of("/"));
        std::string data_id = "";
        if (componentNameVector.size() > 4)
            data_id  = componentNameVector.at(4);

        return data_id;
    }

    bool MongoStorage::shardIndexer (std::list<std::pair<double,double> >coordSet, bool isDeletion) {
        mongocxx::client conn{mongocxx::uri{}};

        auto full_tile_conn = conn[INDEX_DB][FULL_TILE_COLLECTION];
        
        std::string name100;
        std::string name10;
        std::string name1;
        bool need_tesselation = false;
        auto index_db_conn = conn[INDEX_DB][INDEX_COLLECTION];
        int initialTiles=index_db_conn.count(bsoncxx::builder::stream::document{} << finalize);
        
        std::list< std::pair<double,double> >::iterator it;
        
        std::vector<bool> lvl0;
        std::vector<bool> lvl1;
        
        for(it=coordSet.begin(); it!=coordSet.end(); ++it)
        {
            double x = it->first;
            double y = it->second;
        
            std::string tileName = calculateLATLONName(x,y,2);
            
            try
            {   
                /* if(!shard_entry) {
                    bsoncxx::builder::stream::document shard_document{};
                    shard_document << "name" << shardName << "counter" << 1;
                    data_connection.insert_one(shard_document.view());
                    //INSERTED new shard -> Update index version
                    index_changed = true;
                }
                else 
                { 
                    bsoncxx::document::element element = shard_entry->view()["counter"];
                    if(element.type() != bsoncxx::type::k_int32)
                        std::cout << "ERROR retrieving shard_counter "<<  (int)element.type() << std::endl;
                    else
                    {           
                        int counter = element.get_int32();
//                         std::cout << "for shard " << shardName <<" counter was"  << counter << std::endl;
                        
                        if(!isDeletion)
                            data_connection.update_one(bsoncxx::builder::stream::document{} << "name" << shardName << finalize,
                            bsoncxx::builder::stream::document{} << "$set" << bsoncxx::builder::stream::open_document << "counter" << ++counter << bsoncxx::builder::stream::close_document << finalize);
                        else if (isDeletion && counter > 1)
                            data_connection.update_one(bsoncxx::builder::stream::document{} << "name" << shardName << finalize,
                            bsoncxx::builder::stream::document{} << "$set" << bsoncxx::builder::stream::open_document << "counter" <<--counter << bsoncxx::builder::stream::close_document << finalize);
                        else if (isDeletion && counter <= 1)
                        {
                            mongocxx::stdx::optional<mongocxx::result::delete_result> result = data_connection.delete_many(bsoncxx::builder::stream::document{} << "name" << shardName << finalize);
                            //DELETED a shard -> Update index version
                            index_changed = true;
                        }
                        else
                            std::cout << "ERROR: in counter updating for shardName " << shardName << std::endl; 
                    }                        
                }*/
                
                // insert 1x1
                name1= tileName;                      
                std::vector<std::string> componentNameVector;
                if (boost::starts_with(tileName, "/")==false)
                    tileName = "/" + tileName;
                boost::split(componentNameVector, tileName, boost::is_any_of("/"));
                name100 = "/"+componentNameVector[1]+"/"+componentNameVector[2];
                name10  = "/"+componentNameVector[1]+"/"+componentNameVector[2]+"/"+componentNameVector[3];
                
//                 std::cout<<" name1: "<<name1<<" name10: "<<name10<< " name100: "<<name100<<std::endl;
                
                bsoncxx::stdx::optional<bsoncxx::document::value> tile_entry = full_tile_conn.find_one(bsoncxx::builder::stream::document{} << "name1" << name1 << "level" << 2 << finalize);
                if(!isDeletion) { // is an insertion
//                     std::cout<<"is an insertion"<<std::endl;
                    if(!tile_entry) {  // level 2 tile does not exist"
//                         std::cout<<"level 2 tile does not exist"<<std::endl;
                        // before insert NEW lvl 2 tile, check if parent tiles already exist (in order to know if the NEW level 2 tile is leaf or not. Top down approach
                        lvl0 = checkParentIsLeaf(name100, 0);
                        if (!lvl0[0]) { //level 0 parent does not exist, create lvl 0 , 1 and 2 with leaf=true only on lvl 2
//                             std::cout<<"level 0 parent does not exist"<<std::endl;
                            createTileEntry(name1, name10, name100, 0, false);
                            createTileEntry(name1, name10, name100, 1, false);
                            createTileEntry(name1, name10, name100, 2, true);
                        }
                        else if (lvl0[0]) { // level 0 parent already  exists
//                             std::cout<<"level 0 parent already  exists"<<std::endl;
                            lvl1 = checkParentIsLeaf(name10, 1);
                            if (!lvl1[0]) { // level 1 parent does not exist. Check lvl 0 
//                                  std::cout<<"level 1 parent does not exist. Check lvl 0 "<<std::endl;
                                if (lvl0[1]) {// lvl 0 is leaf: update "counter" and "children" on level 0 and create lvl 1 and 2 with leaf=false;
                                    updateExistingTile(name100, 0, true);
                                    createTileEntry(name1, name10, name100, 1, false);
                                    createTileEntry(name1, name10, name100, 2, false);
                                } else { //lvl 0 is NOT leaf: update "counter" and "children" on level 0 and create lvl 1 and 2 with leaf=false only on level 2
                                    updateExistingTile(name100, 0, true);
                                    createTileEntry(name1, name10, name100, 1, false);
                                    createTileEntry(name1, name10, name100, 2, true);
                                }
                            }
                            else {  // level 1 parent already exists.
//                                 std::cout<<"level 1 parent already exists."<<std::endl;
                                if (lvl1[1]) { // lvl 1 is leaf. Create lvl2 with leaf=false and update counters on lvl 0 and 1
                                    updateExistingTile(name100, 0, false);
                                    updateExistingTile(name10, 1, false);
                                    createTileEntry(name1, name10, name100, 2, false);
                                } else { // lvl 1 isn't leaf. Create lvl2 with leaf=true and update counters on lvl 0 and 1
                                    updateExistingTile(name100, 0, false);
                                    updateExistingTile(name10, 1, false);
                                    createTileEntry(name1, name10, name100, 2, true);
                                }
                            }
                        }
                    } else { //level 2 tile already exists. Just increase counter
//                         std::cout<<" level 2 tile already exists. Just increase counter"<<std::endl;
                        updateExistingTile(name1, 2, false);
                        need_tesselation=false;
                        return need_tesselation;
                    }
                }
                else { // is a deletion
//                     std::cout<<"entering in decreaseCounter"<<std::endl;
                    decreaseCounter(name1, name10, name100, maxTessellationLevel);
                }
            }
            catch (...) {       
                std::cout << "SOME ERROR IN shardIndexer! "<< std::endl;
                need_tesselation=false;
                return need_tesselation;
            }
        }
        
      
        int nTiles=index_db_conn.count(bsoncxx::builder::stream::document{} << finalize);
        std::cout << "initialTiles="<<initialTiles<<" nTiles=" << nTiles << " limitMax=" << threshold*1.2 << " limitMin=" << threshold*0.8 << std::endl;
        need_tesselation = (nTiles>threshold*1.2 || nTiles<threshold*0.8) && (nTiles!=initialTiles);
        if (nTiles!=initialTiles && !need_tesselation) versionUpdater(); 
        return need_tesselation;
    }
    
    void MongoStorage::createTileEntry(std::string name1, std::string name10, std::string name100, int level, bool isLeaf) {
        mongocxx::client conn{mongocxx::uri{}};
        auto full_tile_conn = conn[INDEX_DB][FULL_TILE_COLLECTION];
        if (level==0) {
            name10 = "xx";
            name1 = "xx";
        }
        else if (level==1)
            name1 = "xx";
//         std::cout<<"inserting tile "<< name1 << "level "<<level<<std::endl;
        bsoncxx::builder::stream::document tile_document{};
        tile_document <<"name100"<<name100<<"name10"<<name10<< "name1" << name1 << "advertise" << true << "children" << 1 <<"level"<< level << "counter" << 1 << "cancelled" << !isLeaf << "leaf" << isLeaf;
        auto insert_one = full_tile_conn.insert_one(tile_document.view());
        
        if(isLeaf && level==2) {    // insert the lvl2 leaf in index_db in order to announce the new tile
            auto index_db_conn = conn[INDEX_DB][INDEX_COLLECTION];
            bsoncxx::builder::stream::document document{};
            document << "name" << name1 << "counter" << 1000;
            insert_one = index_db_conn.insert_one(document.view());
            //update index version
            //versionUpdater();
        }       
    }
    
    void MongoStorage::updateExistingTile(std::string name, int level, bool newChild){
        mongocxx::client conn{mongocxx::uri{}};
        auto full_tile_conn = conn[INDEX_DB][FULL_TILE_COLLECTION];
        std::string name_key = "";
        if (level==0 ) {     
            name_key = "name100";
            if (newChild) { //// increase both "counter" and "children"
                full_tile_conn.update_one(bsoncxx::builder::stream::document{} << name_key << name << "level" << level << finalize, bsoncxx::builder::stream::document{} << "$inc" << bsoncxx::builder::stream::open_document << "counter" << 1 << "children" << 1 << bsoncxx::builder::stream::close_document << finalize);
            } else {   // increase only "counter"
                full_tile_conn.update_one(bsoncxx::builder::stream::document{} << name_key << name << "level" << level << finalize, bsoncxx::builder::stream::document{} << "$inc" << bsoncxx::builder::stream::open_document << "counter" << 1 << bsoncxx::builder::stream::close_document << finalize);
            }
        } else if (level==1) {    // increase "counter" and "children"
            name_key = "name10";
            full_tile_conn.update_one(bsoncxx::builder::stream::document{} << name_key << name << "level" << level << finalize, bsoncxx::builder::stream::document{} << "$inc" << bsoncxx::builder::stream::open_document << "counter" << 1 << "children" << 1 << bsoncxx::builder::stream::close_document << finalize);
        } else {   // increase only "counter" (in level 2 tiles, "counter" means the number of object inside) 
            name_key = "name1";
            full_tile_conn.update_one(bsoncxx::builder::stream::document{} << name_key << name << "level" << level << finalize, bsoncxx::builder::stream::document{} << "$inc" << bsoncxx::builder::stream::open_document << "counter" << 1 << bsoncxx::builder::stream::close_document << finalize);
        }
    }
    
    void MongoStorage::decreaseCounter(std::string name1, std::string name10, std::string name100, int level) {
        std::string name_key, name;
        int counter = 0;
//         int children = 0;
        
        mongocxx::client conn{mongocxx::uri{}};
        auto full_tile_conn = conn[INDEX_DB][FULL_TILE_COLLECTION];
        if (level==0) {
            name_key = "name100";
            name = name100;
        } else if (level==1) {
            name_key = "name10";
            name = name10;
        } else {
            name_key = "name1";
            name = name1;
        }
        
        bsoncxx::stdx::optional<bsoncxx::document::value> tile_entry = full_tile_conn.find_one(bsoncxx::builder::stream::document{} << name_key << name << "level" << level << finalize);
        
        if(tile_entry) {
//             bsoncxx::document::element element_children = tile_entry->view()["children"];
            bsoncxx::document::element element_counter = tile_entry->view()["counter"];
//             if(element_children.type() != bsoncxx::type::k_int32)
//                 std::cout << "ERROR retrieving tile_children "<<  (int)element_children.type() << std::endl;
//             else
            if(element_counter.type() != bsoncxx::type::k_int32)
                std::cout << "ERROR retrieving tile_counter "<<  (int)element_counter.type() << std::endl;
            else {           
//                 children = element_children.get_int32();
                counter  = element_counter.get_int32();
                if (counter > 1 ) {
//                     std::cout << "decreasing counter of tile" << name << " level " << level << std::endl;
                    full_tile_conn.update_one(bsoncxx::builder::stream::document{} << name_key << name << "level" << level << finalize, bsoncxx::builder::stream::document{} << "$inc" << bsoncxx::builder::stream::open_document << "counter" << -1 << bsoncxx::builder::stream::close_document << finalize);
                } else {
//                     std::cout << "erasing tile" << name << " level " << level << std::endl;
                    mongocxx::stdx::optional<mongocxx::result::delete_result> delete_result = full_tile_conn.delete_one(bsoncxx::builder::stream::document{} << name_key << name << "level" << level << finalize);
                    
                    //delete from index_db 
                    auto index_db_conn = conn[INDEX_DB][INDEX_COLLECTION];
                    mongocxx::stdx::optional<mongocxx::result::delete_result> index_db_delete_result = index_db_conn.delete_one(bsoncxx::builder::stream::document{} << "name" << name << finalize);
                    //update index version
                    //versionUpdater();
                    
                    while (level>0)
                        decreaseCounter(name1, name10, name100, --level);
                    if (level>0)
                        decreaseChildren(name1, name10, name100, level-1);
                }
            }
        }
    }
    
    void MongoStorage::decreaseChildren(std::string name1, std::string name10, std::string name100, int level) {
        std::string name_key, name;
        mongocxx::client conn{mongocxx::uri{}};
        auto full_tile_conn = conn[INDEX_DB][FULL_TILE_COLLECTION];
        if (level==0) {
            name_key = "name100";
            name = name100;
        } else if (level==1) {
            name_key = "name10";
            name = name10;
        } else {
            name_key = "name1";
            name = name1;
        }
        
        full_tile_conn.update_one(bsoncxx::builder::stream::document{} << name_key << name << "level" << level << finalize, bsoncxx::builder::stream::document{} << "$inc" << bsoncxx::builder::stream::open_document << "children" << -1 << bsoncxx::builder::stream::close_document << finalize);  
    }
    
    std::vector<bool> MongoStorage::checkParentIsLeaf(std::string name, int level) {
        // first bool is true if the tile exists, second bool is true if the leaf=true
        std::vector<bool> vb(2, false);
        std::string name_key = "";
        mongocxx::client conn{mongocxx::uri{}};
        auto full_tile_conn = conn[INDEX_DB][FULL_TILE_COLLECTION];
        
        if (level==0)
            name_key = "name100";
        else if (level==1)
            name_key = "name10";
        else
            name_key = "name1";
        
        bsoncxx::stdx::optional<bsoncxx::document::value> tile_entry = full_tile_conn.find_one(bsoncxx::builder::stream::document{} << name_key << name << "level" << level << finalize);
        if(tile_entry) { //parent exists, chef if it's leaf
            vb[0]=true;
            bsoncxx::document::element element = tile_entry->view()["leaf"];
            if(element.get_bool() == true)
                vb[1]=true;
        }
        return vb;
    }
    
    void MongoStorage::versionUpdater() {
        mongocxx::client conn{mongocxx::uri{}};
        auto data_connection = conn[INDEX_DB][INDEX_VERSION_COLLECTION];
        //some shard added update Version 
        bsoncxx::stdx::optional<bsoncxx::document::value> version_document_result = data_connection.find_one(bsoncxx::builder::stream::document{} << "name" << "INDEX_VERSION" << finalize);
        if(!version_document_result)
        {
            bsoncxx::builder::stream::document shard_document{};
            shard_document << "name" << "INDEX_VERSION" << "index_version" << 1;
            data_connection.insert_one(shard_document.view());
            SendVersionInterest(1);
        }
        else
        {
            bsoncxx::document::element element = version_document_result->view()["index_version"];

            if(element.type() != bsoncxx::type::k_int32) 
            {
                std::cout << "ERROR retrieving index_version "<< (int)element.type() << std::endl;
            }
            else
            {
                int index_version_value = element.get_int32();
                //std::cout << "index_version_string "<< index_version_value << std::endl;
                data_connection.update_one(bsoncxx::builder::stream::document{} << "name" << "INDEX_VERSION" << finalize,
                bsoncxx::builder::stream::document{} << "$set" << bsoncxx::builder::stream::open_document << "index_version" << ++index_version_value << bsoncxx::builder::stream::close_document << finalize);
                SendVersionInterest(index_version_value);
            }
        }
    }
    
    std::list<std::pair<double,double>> MongoStorage::calculatePointIndex(const Value& coordinates) {
        std::set<std::pair<double,double> >coordSet;
//         double x = floor(coordinates[0].GetDouble());
//         double y = floor(coordinates[1].GetDouble());
        double x = floor10(coordinates[0].GetDouble(), 2);
        double y = floor10(coordinates[1].GetDouble(), 2);
        
//         std::cout << "in calculatePointIndex - x="<<x<<std::endl;
//         std::cout << "in calculatePointIndex - y="<<y<<std::endl;

        coordSet.insert(std::make_pair(x,y));
        
        std::list<std::pair<double,double>> result (coordSet.begin(), coordSet.end());
        //std::cout<<"result.size"<< result.size() <<std::endl;
        return result;
    }
    
    std::list<std::pair<double,double>> MongoStorage::calculateMultiPointIndex(const Value& coordinates) {
        std::set<std::pair<double,double> >coordSet;
        for (SizeType i = 0; i < coordinates.Size(); i++)
        {
            double x = floor(coordinates[i][0].GetDouble());
            double y = floor(coordinates[i][1].GetDouble());
            coordSet.insert(std::make_pair(x,y));
        }
            
        std::list<std::pair<double,double>> result (coordSet.begin(), coordSet.end());
        return result;
    }

    std::list<std::pair<double,double>> MongoStorage::calculatePolygonIndex(const Value& coordinates) {

        polygon_t polygon;
        box_t box_container;

        double min_x = std::numeric_limits<double>::max();
        double min_y = std::numeric_limits<double>::max();
        double max_x = std::numeric_limits<double>::min(); 
        double max_y = std::numeric_limits<double>::min();
        
        std::set<std::pair<double,double>> coordSet;

        for (SizeType i = 0; i < coordinates[0].Size(); i++) 
        {
            double x = coordinates[0][i][0].GetDouble();
            double y = coordinates[0][i][1].GetDouble();

            bg::append(polygon.outer(), point_t(x, y));

            if (x > max_x)
                    max_x = ceil(x);
            if (x < min_x)
                    min_x = floor(x);
            if (y > max_y)
                    max_y = ceil(y);
            if (y < min_y)
                    min_y = floor(y);
        }

        //std::cout << "(" << min_x << "," << min_y << ") , (" << max_x << "," << max_y << ")" << std::endl;
        
        for (int i = (int)min_x; i < max_x; i++)
        {
            for (int j = (int)min_y; j < max_y; j++)
            {
                box_t box_container;
                box_container.min_corner().set<0>(i);
                box_container.min_corner().set<1>(j);
                box_container.max_corner().set<0>(i+1); 
                box_container.max_corner().set<1>(j+1);
        
                if (boost::geometry::intersects(polygon, box_container))
                    coordSet.insert(std::make_pair(i,j));
            }
        }
        
        std::list<std::pair<double,double>> result (coordSet.begin(), coordSet.end());
        return result;
    }

    std::list<std::pair<double,double>> MongoStorage::calculateGeometryIndex(rapidjson::Document& json_data) {
        const Value& geometry = json_data["geometry"];
        std::string geometry_type = geometry["type"].GetString();
        const Value& coordinates = geometry["coordinates"];
        std::list<std::pair<double,double>> coordSet = std::list<std::pair<double,double>> ();

        if (boost::iequals(geometry_type, "Point") )
        {
            coordSet = calculatePointIndex(coordinates);
        }
        else if (boost::iequals(geometry_type, "MultiPoint") )
        {
            coordSet = calculateMultiPointIndex(coordinates);
        }
        else if (boost::iequals(geometry_type, "Polygon") )
        {
            coordSet = calculatePolygonIndex(coordinates);    
        }
        return coordSet;
    }

    void MongoStorage::calculateIndex(rapidjson::Document& json_data, bool isDeletion) {
//         struct timeval tp;
//         gettimeofday(&tp, NULL);
//         long int start = tp.tv_sec * 1000 + tp.tv_usec / 1000; //get current timestamp in milliseconds

        std::list<std::pair<double,double>> coordSet = calculateGeometryIndex(json_data);
        
        bool need_tesselation = shardIndexer(coordSet, isDeletion);
        if (need_tesselation == true) {
            resetFullTileDB();
            constrainedTesselation();
            buildIndexDB();
        }
//         gettimeofday(&tp, NULL);
//         long int stop = tp.tv_sec * 1000 + tp.tv_usec / 1000; //get current timestamp in milliseconds

        //std::cout << "calculateShards Time: " << stop-start <<" ms" <<std::endl;  
    }

    int64_t MongoStorage::insert(const Data& data) {
        //oName = {sid}/{did}/{uid}/{app suffix}
        mongocxx::client conn{mongocxx::uri{}};   

//         std::cout << "DATA NAME TO INSERT: " << data.getName().toUri() << std::endl; 
        
        Name name = data.getName();
        std::string data_id  = calculateDataIdentifier(name);
        std::string tenant_id  = calculateTenantIdentifier(name);

        if (data_id == "" || tenant_id == "")
            return -1;

        //INSERT data segment in data db
        std::string data_wire = base64_encode(data.wireEncode().wire(), data.wireEncode().size());  
        
        bsoncxx::builder::stream::document data_element{};
        data_element << "name" << name.toUri();
        data_element << "data" << data_wire;
        auto data_connection = conn[tenant_id+DATA_DB_SUFFIX][data_id];
        
        
        bsoncxx::builder::stream::document index_builder;
        index_builder << "name" << 1;
        data_connection.create_index(index_builder.view(), {});
        
        //now take the content and store for OGB search
        std::string fullContent = "";  
        for (u_int i =0; i< data.getContent().value_size(); i++)
            fullContent.push_back(data.getContent().value()[i]);

        //Parsing the data content
        rapidjson::Document json_data;
        if (json_data.Parse(const_cast<char*> (fullContent.c_str())).HasParseError()) {
            fprintf(stderr, "\nError(offset %u): %s\n", 
            (unsigned)json_data.GetErrorOffset(),
            GetParseError_En(json_data.GetParseError()));
            return -1;
        }
        json_data.Parse(const_cast<char*> (fullContent.c_str()));
        std::string ref_str = name.toUri();
        std::string content = fullContent;
        
        if (json_data.HasMember("reference"))
            ref_str = json_data["reference"].GetString();
        
        if (json_data.HasMember("content"))
        {
            //std::cout << "Content found" << std::endl;
            rapidjson::StringBuffer strbuf;
            strbuf.Clear();

            rapidjson::Writer<rapidjson::StringBuffer> writer(strbuf);
            json_data["content"].Accept(writer);

            //now content contains the data content in string 
            content = strbuf.GetString();
        }

        bsoncxx::document::value content_value = bsoncxx::from_json(content);
        bsoncxx::builder::stream::document index_element{};
        index_element << "reference" << ref_str;
        index_element << "name" << name.toUri();
        index_element << "content" << bsoncxx::types::b_document{content_value};
        //std::cout << "content: " << content << std::endl;

        auto index_connection = conn[tenant_id+INDEX_DB_SUFFIX][data_id];
        //index_connection.create_index(index_doc.extract());
        auto index_specification = bsoncxx::builder::stream::document{} << "content.geometry" << "2dsphere" << finalize;
        index_connection.create_index(std::move(index_specification));

        
        //if no error then insert in data e index db
        index_connection.insert_one(index_element.view());
        data_connection.insert_one(data_element.view()); 

        //struct timeval tp;
        //gettimeofday(&tp, NULL);
        //long int start = tp.tv_sec * 1000 + tp.tv_usec / 1000; //get current timestamp in milliseconds
       
        bool isDeletion = false;
        calculateIndex(json_data, isDeletion);

        //gettimeofday(&tp, NULL);
        //long int stop = tp.tv_sec * 1000 + tp.tv_usec / 1000; //get current timestamp in milliseconds
    
    //std::cout<<"indexing time : "<<(stop-start)<<" ms"<<std::endl;
        return 0;
    }

    int64_t MongoStorage::insertLongData(std::string data_string) {
//          struct timeval tp;
//          gettimeofday(&tp, NULL);
//          long int start = tp.tv_sec * 1000 + tp.tv_usec / 1000; //get current timestamp in milliseconds
//          std::cout<<"insertLongData - startTime:"<<start<<std::endl;
        
        rapidjson::Document data_json;
        if (data_json.Parse<rapidjson::kParseStopWhenDoneFlag>(const_cast<char*> (data_string.c_str())).HasParseError()) {
            fprintf(stderr, "\nError(offset %u): %s\n", 
            (unsigned)data_json.GetErrorOffset(),
            GetParseError_En(data_json.GetParseError()));
            return -1;
        }
        data_json.Parse<rapidjson::kParseStopWhenDoneFlag>(const_cast<char*> (data_string.c_str()));
        assert(data_json.HasMember("content"));
        assert(data_json["content"].IsString());
        
//         std::cout<<"correct assertions"<<std::endl;
    
        std::string data_content =data_json["content"].GetString();
//         std::cout<<"data_content parsed, length: "<<data_content.length()<<std::endl;

        boost::replace_all(data_content, "\\\"", "\"");
//         std::cout<<"data_content de-escapated"<<std::endl;
        
    rapidjson::Document content_json;
        content_json.Parse(const_cast<char*> (data_content.c_str()));
        std::string orig_name =content_json["reference"].GetString();
        
        Name name = Name(orig_name);
        name::Component lastComponent = name.get(name.size()-1);
        if(lastComponent.isSegment())
        {
            name = name.getPrefix(-1); 
        }
        std::string data_name = name.toUri();
        
        //INSERT in Data database
        mongocxx::client conn{mongocxx::uri{}};   
        std::string data_id  = calculateDataIdentifier(name);
        std::string tenant_id  = calculateTenantIdentifier(name);

        if (data_id == "" || tenant_id == "")
            return -1;

        bsoncxx::builder::stream::document index_builder;
        index_builder << "name" << 1;
        
        auto data_connection = conn[tenant_id+DATA_DB_SUFFIX][data_id];
        data_connection.create_index(index_builder.view(), {});
        
    const Value& segments = data_json["segments"];
    assert(segments.IsArray());
    for (SizeType i = 0; i < segments.Size(); i++) // Uses SizeType instead of size_t
        {
        Name tmp_name = name;
        tmp_name.appendSegment((int)i).toUri();
//         std::cout<< segments[i].GetString()<<std::endl<<std::endl;
                std::string data_segment = segments[i].GetString();
//                 shared_ptr<Data> data(new Data(Block(reinterpret_cast<const uint8_t*>(data_segment.c_str()), data_segment.length())));
//         std::string data_wire = base64_encode(data->wireEncode().wire(), data->wireEncode().size());  
        bsoncxx::builder::stream::document data_element{};
        data_element << "name" << tmp_name.toUri();
        data_element << "data" << data_segment;       //senza base64_encode perchè lo fa il json mapper prima di creare il zip (nel fes)
                data_connection.insert_one(data_element.view());
    }  
            
    
        std::string content = "";
        //parse JSON object "content" as String
        if (content_json.HasMember("content"))
        {
            //std::cout << "Content found" << std::endl;
            rapidjson::StringBuffer strbuf;
            strbuf.Clear();

            rapidjson::Writer<rapidjson::StringBuffer> writer(strbuf);
            content_json["content"].Accept(writer);

            //now content contains the data content in string 
            content = strbuf.GetString();
        }

        bsoncxx::document::value content_value = bsoncxx::from_json(content);
        bsoncxx::builder::stream::document index_element{};
        index_element << "reference" << orig_name;
        index_element << "name" << orig_name;
        index_element << "content" << bsoncxx::types::b_document{content_value};
        //std::cout << "content: " << content << std::endl;

        auto index_connection = conn[tenant_id+INDEX_DB_SUFFIX][data_id];
        auto index_specification = bsoncxx::builder::stream::document{} << "content.geometry" << "2dsphere" << finalize;
        index_connection.create_index(std::move(index_specification));
        index_connection.insert_one(index_element.view());
        
//         gettimeofday(&tp, NULL);
//         long int stop = tp.tv_sec * 1000 + tp.tv_usec / 1000; //get current timestamp in milliseconds
//         std::cout<<"insertLongData - stoptime:"<<stop<<std::endl;
//         std::cout<<"insertLongData elapsed time: "<<(stop-start)<<" ms"<<std::endl;
        
        return 0;
    }    

    bool MongoStorage::erase(const int64_t id) {
        return false;
    }

    bool MongoStorage::erase(const Name& nameOrig) {

        Name name = Name(nameOrig);
        name::Component lastComponent = name.get(name.size()-1);
        if(lastComponent.isSegment())
        {
            name = name.getPrefix(-1); 
        }
        std::string name_str = name.toUri();
        boost::erase_all(name_str, "/DELETE");
//         std::cout << "NAME: " << name_str << std::endl;

        std::string data_id  = calculateDataIdentifier(name);
        std::string tenant_id  = calculateTenantIdentifier(name);
        
        if (data_id == "" || tenant_id == "")
            return -1;
        
        mongocxx::client conn{mongocxx::uri{}};
        auto collection = conn[tenant_id+INDEX_DB_SUFFIX][data_id];
        bool res = false;
        
        //retrieve geometry and calculate shard list in order to update shard counter
        bool isDeletion = true;
        
        std::stringstream result_stream;
        mongocxx::stdx::optional<bsoncxx::document::value> object_to_delete = collection.find_one(bsoncxx::builder::stream::document{} << "name" << name_str << finalize);
        if(object_to_delete) {
            res = true;
            //std::cout << result->deleted_count() << "\n";
            result_stream << bsoncxx::to_json(*object_to_delete);
        }
        
        
        rapidjson::Document json_data;
        json_data.Parse(const_cast<char*> (result_stream.str().c_str()));
        
        std::string content;
        std::string geoJSON_str;
        if (json_data.HasMember("content"))
        {   
            const Value& geoJSON = json_data["content"];
            rapidjson::Document geoJSON_doc;
            geoJSON_doc.CopyFrom(geoJSON, geoJSON_doc.GetAllocator());
            calculateIndex(geoJSON_doc, isDeletion);
        }
        
        mongocxx::stdx::optional<mongocxx::result::delete_result> result =collection.delete_many(bsoncxx::builder::stream::document{} << "name" << name_str << finalize);
        if(result) {
            res = true;
            //std::cout << result->deleted_count() << "\n";
        }

        auto data_collection = conn[tenant_id+DATA_DB_SUFFIX][data_id];
        
//         std::cout << "PRE TRYNG to delete : " << name_str << std::endl;
        Name prefixName= Name(name_str);
        if(prefixName.get(prefixName.size()-1).isSegment())
        {
            prefixName = prefixName.getPrefix(-1); 
        }
        name_str = prefixName.toUri();
        
//         std::cout << "TRYNG to delete : " << name_str << std::endl;
        //mongocxx::stdx::optional<mongocxx::result::delete_result> data_result = data_collection.delete_many(bsoncxx::builder::stream::document{} << "name" << name_str << finalize);
        mongocxx::stdx::optional<mongocxx::result::delete_result> data_result = data_collection.delete_many(bsoncxx::builder::stream::document{} << "name" << bsoncxx::types::b_regex("^"+name_str,"") << finalize);
        if(data_result) {
            res = true;
//             std::cout <<"DELETED: "<< data_result->deleted_count() << "\n";
        }

        return res; 
    }

    shared_ptr<Data> MongoStorage::read(const Interest& interest) {

        Name name = Name(interest.getName());
        //attualmente non salviamo segmenti per cui ogni richiesta di un segmento la considero una richiesta di tutto il data
//         name::Component lastComponent = name.get(name.size()-1);
        //if(lastComponent.isSegment())
        //{
            //std::cout << " is a SEGMENT: " << std::endl;
        //  name = name.getPrefix(-1); 
        //}

        std::string name_str = name.toUri();
//         std::cout << "NAME: " << name_str << std::endl;


        std::string data_id  = calculateDataIdentifier(name);
        std::string tenant_id  = calculateTenantIdentifier(name);

        mongocxx::client conn{mongocxx::uri{}};
        auto collection = conn[tenant_id+DATA_DB_SUFFIX][data_id];

        mongocxx::stdx::optional<bsoncxx::document::value> mongo_result;
        std::stringstream result_stream;
        mongocxx::options::find opts{};
        opts.projection(bsoncxx::builder::stream::document{} << "data" << 1 << "_id" << 0  << finalize);

        mongo_result = collection.find_one(bsoncxx::builder::stream::document{} << "name" << name_str << finalize, opts);
        if(mongo_result) {
            result_stream << bsoncxx::to_json(*mongo_result);
        }
        else
        {
//             std::cout << "NO result" << std::endl;
            return shared_ptr<Data>();
        }
//         std::cout << "result: " << result_stream.str().c_str() << std::endl;
        //Parsing the data content
        rapidjson::Document json_mongo;
        json_mongo.Parse(const_cast<char*> (result_stream.str().c_str()));

        std::string data_wired;
//         char *data_ptr;
        
//         std::cout << "PRE DATA: " << std::endl;
        if (json_mongo.HasMember("data"))
        {
//             std::cout << "IN DATA " <<std::endl;//<< json_mongo["data"].GetString() << std::endl;
            data_wired = base64_decode(json_mongo["data"].GetString());
            shared_ptr<Data> data(new Data(Block(reinterpret_cast<const uint8_t*>(data_wired.c_str()), data_wired.length())));
            //data->wireDecode(Block(reinterpret_cast<const uint8_t*>(data_wired.c_str()), data_wired.length()));
            //std::cout<<"data name: "<<data->getName().toUri()<<std::endl;
            return data;
        }
//         std::cout << "POST DATA: " << std::endl;
        return shared_ptr<Data>(); 
    }

    shared_ptr<Data> MongoStorage::read(const int64_t id) {
        return shared_ptr<Data>();
    }

    int64_t MongoStorage::size() {
        return 0;
    }

} // namespace repo
