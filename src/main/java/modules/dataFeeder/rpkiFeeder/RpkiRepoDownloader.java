package modules.dataFeeder.rpkiFeeder;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.vertx.core.Vertx;
import modules.helper.DbHandler;
import org.postgresql.util.PGobject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;

public class RpkiRepoDownloader implements Runnable {

    private Vertx vertx;

    public RpkiRepoDownloader() {
    }

    @Override
    public void run() {
        URL url = null;
        HttpURLConnection rpkiValidatorRestApiConnection = null;

        try {
            url = new URL("http://localhost:9176/export.json");

            rpkiValidatorRestApiConnection = (HttpURLConnection) url.openConnection();
            rpkiValidatorRestApiConnection.setRequestMethod("GET");
            rpkiValidatorRestApiConnection.setUseCaches(false);
            rpkiValidatorRestApiConnection.setAllowUserInteraction(false);

            rpkiValidatorRestApiConnection.addRequestProperty("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:57.0) Gecko/20100101 Firefox/57.0");
            rpkiValidatorRestApiConnection.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            int status = rpkiValidatorRestApiConnection.getResponseCode();
            switch(status){
                case 200:
                case 201:
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(rpkiValidatorRestApiConnection.getInputStream()));
                    String inputLine;
                    StringBuilder content = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }
                    JSONObject jsonObject = JSON.parseObject(content.toString());
                    // Raw stirng example {"roa":[
                    // {"asn":"44489","prefix":"185.131.60.0/22","maxLength":24,"ta":"RIPE NCC RPKI Root"},
                    // ]}
                    JSONArray roasArray = jsonObject.getJSONArray("roa");

                    try(Connection dbConnection = DbHandler.produceConnection()){
                        assert dbConnection != null;
                        Statement stmt = dbConnection.createStatement();
//                        ResultSet rs = stmt.executeQuery("CREATE TABLE validated_roas_rv_test ( id SERIAL (10) DEFAULT nextval('validated_roas_rv_test_id_seq':: REGCLASS ) NOT NULL\n" + "  CONSTRAINT validated_roas_rv_test_pkey\n" + "  PRIMARY KEY,\n" + "  asn          CIDR(max)    NOT NULL, max_length   INT4(10)     NOT NULL,  trust_anchor VARCHAR(255) NOT NULL\n" + "); COMMENT ON TABLE validated_roas_rv_test IS 'test table to be filled from the rpki validator run'");
                        ResultSet rs = stmt.executeQuery("CREATE TABLE validated_roas_rv_test\n" +
                                "(\n" +
                                "\troa_id INT4(10) NOT NULL\n" +
                                "\t\tCONSTRAINT validated_roas_rv_test_pkey\n" +
                                "\t\t\tPRIMARY KEY,\n" +
                                "\tasn INT4(10) NOT NULL,\n" +
                                "\tprefix CIDR(max) NOT NULL,\n" +
                                "\tmax_length INT4(10),\n" +
                                "\ttrust_anchor VARCHAR(255) NOT NULL);");
                        dbConnection.commit();


                        String insertStmt = "INSERT INTO validated_roas_rv_test VALUES (?, ?, ?, ?)";
                        PreparedStatement ps = dbConnection.prepareStatement(insertStmt);

                        for(Object value : roasArray  ){
                            JSONObject json_roa = (JSONObject) value;
                            ps.setInt(1, json_roa.getIntValue("asn"));

                            PGobject dummyObject = new PGobject();
                            dummyObject.setType("cidr");
                            dummyObject.setValue(json_roa.getString ("prefix"));
                            ps.setObject(2, dummyObject, Types.OTHER);
                            ps.setInt(3, json_roa.getIntValue("maxLength"));
                            ps.setString(4, json_roa.getString("ta"));

                        }


                    } catch (SQLException e) {
                        e.printStackTrace();
                    }


                    in.close();

                default:
            }


        } catch (IOException e) {
            e.printStackTrace();
        } finally{
            assert rpkiValidatorRestApiConnection != null;
            rpkiValidatorRestApiConnection.disconnect();

        }

    }
}
