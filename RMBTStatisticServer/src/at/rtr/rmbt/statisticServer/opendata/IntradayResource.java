package at.rtr.rmbt.statisticServer.opendata;

import at.rtr.rmbt.shared.cache.CacheHelper;
import at.rtr.rmbt.shared.db.opendata.QueryParser;
import at.rtr.rmbt.statisticServer.ServerResource;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Form;
import org.restlet.resource.Get;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

public class IntradayResource extends ServerResource {
    private static final int CACHE_EXP = 3600;
    private final CacheHelper cache = CacheHelper.getInstance();


    @Get("json")
    public String request(final String entity) throws JSONException {
        addAllowOrigin();
        QueryParser qp = new QueryParser();
        qp.getAllowedFields().put("quantile", QueryParser.FieldType.IGNORE);

        final Form parameters = getRequest().getResourceRef().getQueryAsForm();
        double quantile = 0.5;
        if (parameters.getNames().contains("quantile")) {
            try {
                quantile = Double.parseDouble(parameters.getFirstValue("quantile"));
                quantile = Math.min(quantile,1);
                quantile = Math.max(quantile,0);
            }
            catch (NumberFormatException e) { }
        }

        //set transformator for time to allow for broader caching
        qp.registerSingleParameterTransformator("time", new QueryParser.SingleParameterTransformator() {
            private final static int ONE_HOUR = 60*60*1000;

            @Override
            public void transform(QueryParser.SingleParameter param) {
                //round to 1h
                long timestamp = Long.parseLong(param.getValue());
                timestamp = timestamp - (timestamp % ONE_HOUR);
                param.setValue(Long.toString(timestamp));
            }
        });

        qp.parseQuery(formToMultimap(parameters));

        //try cache first
        String cacheKey = "opentest-hourly-" + "-" + qp.hashCode() + "-" + quantile;
        String cacheString = (String) cache.get(cacheKey);
        if (cacheString != null) {
            //System.out.println("cache hit for hourly");
            return cacheString;
        }

        List<HourlyStatistic> statistics = queryDb(qp, quantile);

        JSONArray ret = new JSONArray();

        for (HourlyStatistic stats : statistics) {
            ret.put(stats.toJson());
        }


        //put in cache
        cache.set(cacheKey, CACHE_EXP, ret.toString());

        return ret.toString();
    }

    private List<HourlyStatistic> queryDb(QueryParser qp, double quantile) {
        String sql = "SELECT" +
                "  count(t.open_test_uuid)," +
                "  extract(hour from t.time AT TIME ZONE t.timezone) AS hour," +
                "  quantile(t.speed_download :: bigint, ?)          quantile_down," +
                "  quantile(t.speed_upload :: bigint, ?)            quantile_up," +
                "  quantile(t.ping_median :: bigint, ?)             quantile_ping" +
                " FROM test t" +
                qp.getJoins() +
                " WHERE t.deleted = false" +
                " AND status = 'FINISHED' " + qp.getWhereClause("AND") +
                " GROUP BY hour;";

        List<HourlyStatistic> ret = new LinkedList<>();

        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setDouble(1, quantile);
            ps.setDouble(2, quantile);
            ps.setDouble(3, quantile);
            qp.fillInWhereClause(ps, 4);
            //System.out.println(ps);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                HourlyStatistic stats = new HourlyStatistic(rs.getDouble("quantile_down"),
                        rs.getDouble("quantile_up"),
                        rs.getDouble("quantile_ping"),
                        rs.getFloat("hour"),
                        rs.getLong("count"));
                ret.add(stats);
            }
            ps.close();


        } catch (SQLException e) {
            e.printStackTrace();
        }
        return  ret;

    }

    public static class HourlyStatistic {
        private final double quantileDown;
        private final double quantileUp;
        private final double quantilePing;
        private final float hourOfTheDay;
        private final long count;

        public HourlyStatistic(double quantileDown, double quantileUp, double quantilePing, float hourOfTheDay, long count) {
            this.quantileDown = quantileDown;
            this.quantileUp = quantileUp;
            this.quantilePing = quantilePing;
            this.hourOfTheDay = hourOfTheDay;
            this.count = count;
        }


        public JSONObject toJson() {
            JSONObject ret = new JSONObject();
            ret.put("hour", getHourOfTheDay());
            ret.put("quantile_down",getQuantileDown());
            ret.put("quantile_up",getQuantileUp());
            ret.put("quantile_ping",getQuantilePing());
            ret.put("count",getCount());
            return ret;
        }

        public double getQuantileDown() {
            return quantileDown;
        }

        public double getQuantileUp() {
            return quantileUp;
        }

        public double getQuantilePing() {
            return quantilePing;
        }

        public float getHourOfTheDay() {
            return hourOfTheDay;
        }

        public long getCount() {
            return count;
        }
    }
}
