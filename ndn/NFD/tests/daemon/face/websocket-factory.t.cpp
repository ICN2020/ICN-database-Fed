/* -*- Mode:C++; c-file-style:"gnu"; indent-tabs-mode:nil; -*- */
/**
 * Copyright (c) 2014-2017,  Regents of the University of California,
 *                           Arizona Board of Regents,
 *                           Colorado State University,
 *                           University Pierre & Marie Curie, Sorbonne University,
 *                           Washington University in St. Louis,
 *                           Beijing Institute of Technology,
 *                           The University of Memphis.
 *
 * This file is part of NFD (Named Data Networking Forwarding Daemon).
 * See AUTHORS.md for complete list of NFD authors and contributors.
 *
 * NFD is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * NFD is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * NFD, e.g., in COPYING.md file.  If not, see <http://www.gnu.org/licenses/>.
 */

#include "face/websocket-factory.hpp"

#include "factory-test-common.hpp"
#include "face-system-fixture.hpp"
#include "tests/limited-io.hpp"

namespace nfd {
namespace face {
namespace tests {

using namespace nfd::tests;
namespace ip = boost::asio::ip;

BOOST_AUTO_TEST_SUITE(Face)
BOOST_FIXTURE_TEST_SUITE(TestWebSocketFactory, BaseFixture)

BOOST_FIXTURE_TEST_SUITE(ProcessConfig, FaceSystemFixture)

BOOST_AUTO_TEST_CASE(Normal)
{
  const std::string CONFIG = R"CONFIG(
    face_system
    {
      websocket
      {
        listen yes
        port 9696
        enable_v4 yes
        enable_v6 yes
      }
    }
  )CONFIG";

  parseConfig(CONFIG, true);
  parseConfig(CONFIG, false);

  auto& factory = this->getFactoryById<WebSocketFactory>("websocket");
  checkChannelListEqual(factory, {"ws://[::]:9696"});
}

BOOST_AUTO_TEST_CASE(EnableIpv4Only)
{
  const std::string CONFIG = R"CONFIG(
    face_system
    {
      websocket
      {
        listen yes
        port 9696
        enable_v4 yes
        enable_v6 no
      }
    }
  )CONFIG";

  parseConfig(CONFIG, true);
  parseConfig(CONFIG, false);

  auto& factory = this->getFactoryById<WebSocketFactory>("websocket");
  checkChannelListEqual(factory, {"ws://0.0.0.0:9696"});
}

BOOST_AUTO_TEST_CASE(UnsupportedIpv6Only)
{
  const std::string CONFIG = R"CONFIG(
    face_system
    {
      websocket
      {
        listen yes
        port 9696
        enable_v4 no
        enable_v6 yes
      }
    }
  )CONFIG";

  BOOST_CHECK_THROW(parseConfig(CONFIG, true), ConfigFile::Error);
  BOOST_CHECK_THROW(parseConfig(CONFIG, false), ConfigFile::Error);
}

BOOST_AUTO_TEST_CASE(ChannelsDisabled)
{
  const std::string CONFIG = R"CONFIG(
    face_system
    {
      websocket
      {
        listen yes
        port 9696
        enable_v4 no
        enable_v6 no
      }
    }
  )CONFIG";

  BOOST_CHECK_THROW(parseConfig(CONFIG, true), ConfigFile::Error);
  BOOST_CHECK_THROW(parseConfig(CONFIG, false), ConfigFile::Error);
}

BOOST_AUTO_TEST_CASE(NoListen)
{
  const std::string CONFIG = R"CONFIG(
    face_system
    {
      websocket
      {
        listen no
        port 9696
        enable_v4 yes
        enable_v6 yes
      }
    }
  )CONFIG";

  auto& factory = this->getFactoryById<WebSocketFactory>("websocket");
  BOOST_CHECK_EQUAL(factory.getChannels().size(), 0);
}

BOOST_AUTO_TEST_CASE(ChangeEndpoint)
{
  const std::string CONFIG1 = R"CONFIG(
    face_system
    {
      websocket
      {
        port 9001
      }
    }
  )CONFIG";

  parseConfig(CONFIG1, false);
  auto& factory = this->getFactoryById<WebSocketFactory>("websocket");
  checkChannelListEqual(factory, {"ws://[::]:9001"});

  const std::string CONFIG2 = R"CONFIG(
    face_system
    {
      websocket
      {
        port 9002
      }
    }
  )CONFIG";

  parseConfig(CONFIG2, false);
  checkChannelListEqual(factory, {"ws://[::]:9001", "ws://[::]:9002"});
}

BOOST_AUTO_TEST_SUITE_END() // ProcessConfig

BOOST_AUTO_TEST_CASE(GetChannels)
{
  WebSocketFactory factory;
  BOOST_REQUIRE_EQUAL(factory.getChannels().empty(), true);

  std::vector<shared_ptr<const Channel>> expectedChannels;
  expectedChannels.push_back(factory.createChannel("127.0.0.1", "20070"));
  expectedChannels.push_back(factory.createChannel("127.0.0.1", "20071"));
  expectedChannels.push_back(factory.createChannel("::1", "20071"));

  for (const auto& i : factory.getChannels()) {
    auto pos = std::find(expectedChannels.begin(), expectedChannels.end(), i);
    BOOST_REQUIRE(pos != expectedChannels.end());
    expectedChannels.erase(pos);
  }

  BOOST_CHECK_EQUAL(expectedChannels.size(), 0);
}

BOOST_AUTO_TEST_CASE(UnsupportedFaceCreate)
{
  WebSocketFactory factory;

  createFace(factory,
             FaceUri("ws://127.0.0.1:20070"),
             ndn::nfd::FACE_PERSISTENCY_PERMANENT,
             false,
             {CreateFaceExpectedResult::FAILURE, 406, "Unsupported protocol"});

  createFace(factory,
             FaceUri("ws://127.0.0.1:20070"),
             ndn::nfd::FACE_PERSISTENCY_ON_DEMAND,
             false,
             {CreateFaceExpectedResult::FAILURE, 406, "Unsupported protocol"});

  createFace(factory,
             FaceUri("ws://127.0.0.1:20070"),
             ndn::nfd::FACE_PERSISTENCY_PERSISTENT,
             false,
             {CreateFaceExpectedResult::FAILURE, 406, "Unsupported protocol"});
}

BOOST_AUTO_TEST_SUITE_END() // TestWebSocketFactory
BOOST_AUTO_TEST_SUITE_END() // Face

} // namespace tests
} // namespace face
} // namespace nfd
