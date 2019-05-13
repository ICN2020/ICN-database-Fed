# @author Bonvoyage/ICN2020 Univ. Tor Vergata Roma Project team

import json
import time
import traceback
import datetime


import requests


class OgbClientLib:
    def __init__(self, server_url):
        self.FrontEndServerURL = server_url

    @staticmethod
    def is_valid_json(json_obj):
        try:
            json.loads(json_obj)
            return True
        except ValueError:
            return False
        except Exception:
            return False

    def login(self, user_id, tenant, password):
        try:
            headers = {"Content-Type" : "application/json"}
            login_params = {"tenantName": tenant, "userName": user_id, "password": password}
            url = self.FrontEndServerURL + "/OGB/user/login"
            r = requests.post(url, data=json.dumps(login_params), headers=headers)
            if r is not None:
                if 199 < r.status_code < 300:
                    return r.json()["token"]
        except Exception:
            print(traceback.format_exc())
        return None

    def insert_geo_json(self, token, cid, geoJSON):
        try:
            url = self.FrontEndServerURL+"/OGB/content/insert/"+cid
            headers = {"Content-Type": "application/json", "Authorization": token}
            r = requests.post(url, data=geoJSON, headers=headers)
            if r is not None:
                if r.status_code == 200:
                    return r.json()["oid"]
                else:
                    print("Insertion failed")
                    print("SERVER REPLY, code: " + str(r.status_code) + " response: " + r.json()["response"])
                    return None
        except Exception:
            print(traceback.format_exc())
        return None

    def delete_object(self, token, oid):
        try:
            url = self.FrontEndServerURL + "/OGB/content/delete/"
            headers = {"Content-Type": "application/json", "Authorization": token}
            body = {"oid"+oid}
            r = requests.post(url, data=body, headers=headers)
            if r is not None:
                if r.status_code == 200:
                    return True
                else:
                    print("Delete failed")
                    print("SERVER REPLY, code: " + str(r.status_code) + " response: " + r.json()["response"])
            return False
        except Exception:
            print(traceback.format_exc())
            return False

    def query_object(self, token, cid, oid):
        try:
            url = self.FrontEndServerURL + "/OGB/query-service/element/" + cid
            headers = {"Content-Type": "application/json", "Authorization": token}
            body = {"oid" : oid}
            r = requests.post(url, data=json.dumps(body), headers=headers)
            if r is not None:
                if r.status_code == 200:
                    return json.dumps(r.json())
                else:
                    print("Query failed")
                    print("SERVER REPLY, code: " + str(r.status_code) + " response: " + json.dumps(r.text))
            return None
        except Exception:
            print(traceback.format_exc())
            return None

    def query_geo_json(self, token, cid, geoJSON):
        try:
            if not self.is_valid_json(geoJSON):
                print("Not valid JSON")
                return None
            url = self.FrontEndServerURL + "/OGB/query-service/" + cid
            headers = {"Content-Type": "application/json", "Authorization": token}
            r = requests.post(url, data=geoJSON, headers=headers)
            if r is not None:
                if r.status_code == 200:
                    return r.content
                else:
                    print("Query failed")
                    print("SERVER REPLY, code: " + str(r.status_code) + " response: " + r.json()["response"])
            return None
        except Exception:
            print(traceback.format_exc())
            return None

    def add_geo_json(self, token, cid, geoJSON):
        try:
            if not self.is_valid_json(geoJSON):
                print("Not valid JSON")
                return None
            oid = self.insert_geo_json(token, cid, geoJSON)
            return oid
        except Exception:
            print(traceback.format_exc())
            return None

    def add_point(self, token, cid, properties_map, location):
        lat = location[0]
        lon = location[1]
        json_obj = {"geometry": {"type": "Point", "coordinates": [str(lon), str(lat)]},
                    "type": "Feature"}
        properties = {}
        for property in properties_map:
            for key, value in property.items():
                properties[key] = value
        json_obj["properties"] = properties
        if not self.is_valid_json(json_obj):
            print("Not valid JSON")
            return None
        return self.insert_geo_json(token, cid, json_obj)


if __name__ == "__main__":
        client = OgbClientLib("http://20.43.77.137:80")
        tid = "icn2020"
        pwd = "icn2020"
        uid = "admin"
        cid = "testCID"
        token = client.login(uid, tid, pwd)

        #JSON = "{\"type\": \"Feature\",\"id\": \"CRAT Rome Soloist\",\"geometry\": {\"type\": \"Polygon\",\"coordinates\": [[[12.0, 41],[13, 41],[13,42],[12, 42],[12.0, 41]]]},\"modalities\": [ \"Walking\", \"Biking\", \"Driving\", \"Public\" ],\"transitions\": [{\"descrition\": \"Rome Termini Train Station\",\"modalities\": [ \"Public\" ],\"geometry\": {\"type\": \"Point\",\"coordinates\": [12.502054, 41.901019]}},{\"descrition\": \"Rome Tiburtina Train Station\",\"modalities\": [ \"Public\" ],\"geometry\": {\"type\": \"Point\",\"coordinates\": [12.531126, 41.911081]}}],\"properties\": {\"url\": \"http://82.223.67.189/bonvoyage/public/api/Rome/\",\"type\": \"SERVICE\",\"sub-type\": \"SOLOIST\",\"timeDependent\": false,\"parallelRouting\": \"OneToOne\",\"multipleRoutes\": true}}";

        prop1 = {"prop100": "value100"}
        prop = [prop1]
        coordinates = [0.1, 0.1]
        oid = client.add_point(token, cid, prop, coordinates)
        # print(client.query_object(token,cid,"/repo/repo/testcid/icn2020/admin/2831629781/%00%00"))
        print(oid)
        time.sleep(1)
        print(client.query_object(token, cid, oid))
