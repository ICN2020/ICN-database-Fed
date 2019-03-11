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

#ifndef REPO_HANDLES_READ_HANDLE_HPP
#define REPO_HANDLES_READ_HANDLE_HPP

#include "base-handle.hpp"

#include "rapidjson/document.h"
#include "rapidjson/writer.h"
#include "rapidjson/stringbuffer.h"

#include <boost/algorithm/string/join.hpp>

#include <bsoncxx/builder/stream/document.hpp>
#include <bsoncxx/json.hpp>
#include <mongocxx/client.hpp>
#include <mongocxx/instance.hpp>

#include <bsoncxx/builder/basic/array.hpp>
#include <bsoncxx/builder/basic/document.hpp>
#include <bsoncxx/builder/basic/kvp.hpp>
#include <bsoncxx/types.hpp>
#include <bsoncxx/exception/exception.hpp>

#include <ndn-cxx/signature.hpp>

#include "HappyHTTP/happyhttp.h"
#include "zlib/zlib.h"


namespace repo {

class ReadHandle : public BaseHandle
{

public:
  ReadHandle(Face& face, RepoStorage& storageHandle, KeyChain& keyChain,
             Scheduler& scheduler)
    : BaseHandle(face, storageHandle, keyChain, scheduler)
  {
  }

  virtual void
  listen(const Name& prefix);

  void
  makeLongInsert(std::string data_string);

private:
  /**
   * @brief Read data from backend storage
   */
  void
  onInterest(const Name& prefix, const Interest& interest);

  void 
  requestInsert(const Interest& interest);

  void
  onRegisterFailed(const Name& prefix, const std::string& reason);

  void 
  sendData(Name name, std::string content);

  void 
  readQuery(const Interest& interest);

  void 
  readVersion(const Interest& interest);

  
};

} // namespace repo

#endif // REPO_HANDLES_READ_HANDLE_HPP
