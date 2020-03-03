package org.camunda.bpm.extension.hooks.services.analytics;


import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service class for publishing data to downstream analytics system.
 *
 * @author  sumathi.thirumani@aot-technologies.com
 */
@Service("dbdatapipeline")
public class SimpleDBDataPipeline extends AbstractDataPipeline {

    private final Logger LOGGER = Logger.getLogger(SimpleDBDataPipeline.class.getName());

    @Autowired
    private NamedParameterJdbcTemplate  analyticsJdbcTemplate;

    /**
     * Transformation method.
     *
     * @param variables
     * @return
     */
    @Override
    public Map<String, Object> prepare(Map<String, Object> variables) {
        LOGGER.info("Inside transformation for pid :"+ getIdentityKey(variables) +" : map: "+variables);
        Map<String,Object> dataMap = new HashMap<>();
        for(Map.Entry<String,Object> entry : variables.entrySet()) {
                    if(entry.getValue() != null) {
                        if(StringUtils.endsWith(entry.getKey(),"_date") || StringUtils.endsWith(entry.getKey(),"_date_time")) {
                            if(StringUtils.isNotEmpty(String.valueOf(entry.getValue()))) {
                                DateTime ts = new DateTime(String.valueOf(entry.getValue()));
                                dataMap.put(entry.getKey(), new Timestamp((ts.getMillis())));
                            }
                        } else {
                            dataMap.put(entry.getKey(), entry.getValue());
                        }

                }
        }
        LOGGER.info("Post transformation:"+ dataMap);
        return dataMap;
    }

    /**
     * Implementation method for direct database connectivity with downstream system.
     * This method handles lob & non-lob objects separately to keep the thread span short.
     *
     * @param data
     * @return
     */
    @Override
    public DataPipelineResponse publish(Map<String,Object> data) {
        DataPipelineResponse response = new DataPipelineResponse();
        Map<String,Object> nonLobMap = new HashMap<>();
        Map<String,Object> lobMap = new HashMap<>();
        try {
            for(Map.Entry<String,Object> entry : data.entrySet()) {
                if(StringUtils.endsWith(entry.getKey(),"_file")) {
                    lobMap.put(entry.getKey(), entry.getValue());
                } else {
                    nonLobMap.put(entry.getKey(), entry.getValue());
                }
            }
            //Non-lob objects block
            String query = getQuery(String.valueOf(nonLobMap.get("entity_key")),nonLobMap);
            LOGGER.info("Non-lob query:"+ query);
            analyticsJdbcTemplate.update(query,nonLobMap);
            // Lob objects
            handleFileObject(String.valueOf(data.get("entity_key")), getIdentityKey(data), lobMap);
            response.setStatus(ResponseStatus.SUCCESS);
        } catch(Exception ex) {
            LOGGER.log(Level.SEVERE, "Exception occurred in publishing data for analytics system", ex);
            response.setStatus(ResponseStatus.FAILURE, ex);
        }
        return response;
    }


    /**
     * Implementation method for notification of execution status.
     *
     * @param response
     * @return
     */
    @Override
    public Map<String,Object> notificationMessage(DataPipelineResponse response) {
        Map<String,Object> rspVarMap = new HashMap<>();
        LOGGER.info("Data pipeline status:" +response.getResponseCode());
        rspVarMap.put("code",response.getResponseCode());
        rspVarMap.put("message",response.getResponseMessage());
        rspVarMap.put("exception",response.getException());
        return rspVarMap;
    }

    /**
     * Method to handle updates of large objects as independent SQL statements.
     *
     * @param entityKey
     * @param id
     * @param lobMap
     * @throws SQLException
     */
    private void handleFileObject(String entityKey, String id, Map<String,Object> lobMap) throws SQLException {
        for(Map.Entry<String,Object> entry : lobMap.entrySet()) {
            Map<String,Object> itrMap = new HashMap<>();
            itrMap.put("pid", id);
            itrMap.put(entry.getKey(), entry.getValue());
            String query = getQuery(entityKey,itrMap);
            LOGGER.info("lob query:"+ query);
            analyticsJdbcTemplate.update(query,itrMap);
        }
    }

    /**
     *  Returns the query bound with key.
     * @param key
     * @return
     */
    private String getQuery(String key) {
        return IQueryFactory.getValidationQuery(key);
    }

    /**
     *  Returns the query
     * @param formKey
     * @param dataMap
     * @return
     * @throws SQLException
     */
    private String getQuery(String formKey, Map<String,Object> dataMap) throws SQLException {
        Map<String,Object> cols = getColumns(formKey, getIdentityKey(dataMap));
        List<String> filteredCols = new ArrayList<>();
        LOGGER.info("Prepare query for columns:"+cols);
        for(Map.Entry<String,Object> entry : dataMap.entrySet()) {
            if(cols.containsKey(entry.getKey().toLowerCase())) {
                filteredCols.add(entry.getKey());
            }
        }
        LOGGER.info("Value of expression"+StringUtils.isEmpty(getIdentityKey(cols)));
        return IQueryFactory.prepareQuery(formKey,filteredCols,StringUtils.isEmpty(getIdentityKey(cols))? Boolean.FALSE : Boolean.TRUE);
    }


    /**
     *  This method returns the column metadata for preparing dynamic queries.
     *
     * @param formKey
     * @param pid
     * @return
     * @throws SQLException
     */
    private Map<String,Object> getColumns(String formKey,String pid) throws SQLException {
            SqlParameterSource namedParameters = new MapSqlParameterSource("pid", pid);
                Map<String, Object> resp = analyticsJdbcTemplate.query(getQuery(formKey), namedParameters,new ResultSetExtractor<Map<String,Object>>(){
                    @Override
                    public Map<String,Object> extractData(ResultSet rs) throws SQLException, DataAccessException {
                        Map<String,Object> dataMap=new HashMap<>();
                        ResultSetMetaData resultSetMetaData = rs.getMetaData();
                        for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
                            dataMap.put(rs.getMetaData().getColumnName(i).toLowerCase(), null);
                        }
                      while(rs.next()) {
                        for (int j = 1; j <= resultSetMetaData.getColumnCount(); j++) {
                            if(StringUtils.endsWith(rs.getMetaData().getColumnName(j),"_file")) {
                                //Not-loading the lob objects on retrieve to keep the metadata lightweight.
                                dataMap.put(rs.getMetaData().getColumnName(j), null);
                            } else {
                                dataMap.put(rs.getMetaData().getColumnName(j), JdbcUtils.getResultSetValue(rs, j));
                            }
                        }
                    }
                        return dataMap;
                    }
                });
        return resp;
    }

}
