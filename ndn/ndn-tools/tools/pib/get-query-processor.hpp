/* -*- Mode:C++; c-file-style:"gnu"; indent-tabs-mode:nil; -*- */
/**
 * Copyright (c) 2014-2015,  Regents of the University of California.
 *
 * This file is part of ndn-tools (Named Data Networking Essential Tools).
 * See AUTHORS.md for complete list of ndn-tools authors and contributors.
 *
 * ndn-tools is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * ndn-tools is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * ndn-tools, e.g., in COPYING.md file.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @author Yingdi Yu <yingdi@cs.ucla.edu>
 */

#ifndef NDN_TOOLS_PIB_GET_QUERY_PROCESSOR_HPP
#define NDN_TOOLS_PIB_GET_QUERY_PROCESSOR_HPP

#include "pib-db.hpp"
#include "encoding/get-param.hpp"
#include <ndn-cxx/interest.hpp>
#include <utility>

namespace ndn {
namespace pib {

/// @brief implements the PIB service
class GetQueryProcessor : noncopyable
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

  /**
   * @brief Constructor
   *
   * @param db The pib database.
   * @param owner Owner of the pib database.
   */
  explicit
  GetQueryProcessor(PibDb& db);

  std::pair<bool, Block>
  operator()(const Interest& interest);

private:
  std::pair<bool, Block>
  processGetUserQuery(const GetParam& param);

  std::pair<bool, Block>
  processGetIdQuery(const GetParam& param);

  std::pair<bool, Block>
  processGetKeyQuery(const GetParam& param);

  std::pair<bool, Block>
  processGetCertQuery(const GetParam& param);

private:
  static const Name PIB_PREFIX;
  static const size_t GET_QUERY_LENGTH;

  const PibDb&  m_db;
};

} // namespace pib
} // namespace ndn

#endif // NDN_TOOLS_PIB_GET_QUERY_PROCESSOR_HPP
