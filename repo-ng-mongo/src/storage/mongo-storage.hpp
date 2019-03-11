#ifndef REPO_STORAGE_MONGO_STORAGE_HPP
#define REPO_STORAGE_MONGO_STORAGE_HPP

#include "storage.hpp"

#include <stdlib.h>
#include <vector>
#include <thread> 

#include "rapidjson/document.h"
#include "rapidjson/writer.h"
#include "rapidjson/error/en.h"

#include <bsoncxx/json.hpp>

#include <mongocxx/client.hpp>
#include <mongocxx/instance.hpp>

#include <ndn-cxx/face.hpp>

#include <boost/geometry.hpp>


using namespace rapidjson;


std::string base64_encode(unsigned char const* , unsigned int len);
std::string base64_decode(std::string const& s);

namespace repo {

using std::queue;
using ndn::Signature;

class MongoStorage : public Storage
{
public:
  class Error : public std::runtime_error
  {
  public:
    explicit
    Error(const std::string& what)
      : std::runtime_error(what)
    {
    }
  };

  explicit
  MongoStorage(const std::string& dbPath);

  virtual
  ~MongoStorage();

  /**
   *  @brief  put the data into database
   *  @param  data     the data should be inserted into databse
   *  @return int64_t  the id number of each entry in the database
   */
  virtual int64_t
  insert(const Data& data);

  virtual int64_t
  insertLongData(std::string data_string);
  
  /**
   *  @brief  remove the entry in the database by using id
   *  @param  id   id number of each entry in the database
   */
  virtual bool
  erase(const int64_t id);

  /**
   *  @brief  remove the entry in the database by using id
   *  @param  name   the name of the item to remove
   */
  virtual bool
  erase(const Name& name);


  /**
   *  @brief  get the data from database
   *  @para   id   id number of each entry in the database, used to find the data
   */
  virtual std::shared_ptr<Data>
  read(const int64_t id);

  /**
  *  @brief  get the data from database
  *  @param  interest   the interest received, used to find the data
  */
  virtual std::shared_ptr<Data>
  read(const Interest& interest);

  /**
   *  @brief  return the size of database
   */
  virtual int64_t
  size();

  /**
   *  @brief enumerate each entry in database and call the function
   *         insertItemToIndex to reubuild index from database
   */
  void
  fullEnumerate(const std::function<void(const Storage::ItemMeta)>& f);

public:

  
private:
  void
  SendVersionInterest(int version);
    
  void 
  sendVersion();
    
  void
  initializeRepo();
  
  void
  onData(const Interest& interest, const Data& data);
  
  void
  onTimeout(const Interest& interest);

  std::string
  calculateDataIdentifier(Name name);

  std::string 
  calculateTenantIdentifier(Name name);

  bool 
  shardIndexer (std::list<std::pair<double,double> >coordSet, bool isDeletion);

  void
  versionUpdater();
  
  void
  buildIndexDB();

  std::list<std::pair<double,double>> 
  calculatePointIndex(const Value& coordinates);
  
  std::list<std::pair<double,double>> 
  calculateMultiPointIndex(const Value& coordinates);

  std::list<std::pair<double,double>> 
  calculatePolygonIndex(const Value& coordinates);

  std::list<std::pair<double,double>> 
  calculateGeometryIndex(rapidjson::Document& json_data);

  void 
  calculateIndex(rapidjson::Document& json_data, bool isDeletion);

  void
  checkVersion();
  
  void
  FullTileCollectionRestore(std::map<std::string,std::pair<int,int>> coordMap1,std::map<std::string,std::pair<int,int>> coordMap10,std::map<std::string,std::pair<int,int>> coordMap100);
  
  void
  resetFullTileDB();
  
  std::vector<bool>
  checkParentIsLeaf(std::string name, int level);
  
  void
  updateExistingTile(std::string name, int level, bool newChild);
  
  void
  createTileEntry(std::string name1, std::string name10, std::string name100, int level, bool isleaf);
  
  void
  decreaseCounter(std::string name1, std::string name10, std::string name100, int level);
      
  void
  decreaseChildren(std::string name1, std::string name10, std::string name100, int level);
  
//   void 
//   FullTileCollectionRestore(std::map<std::string, int> coordMap);

private:
  
  mongocxx::instance _instance{}; // This should be done only once.
  std::string m_dbPath;
  int64_t m_size;
  ndn::Face m_face;
  
};


} // namespace repo

#endif // REPO_STORAGE_MONGO_STORAGE_HPP
