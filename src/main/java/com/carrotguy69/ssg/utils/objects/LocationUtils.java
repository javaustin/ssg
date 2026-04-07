package com.carrotguy69.ssg.utils.objects;

import com.carrotguy69.cxyz.utils.ObjectUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LocationUtils {

    public static List<Location> getLocationsFromYML(List<Map<?, ?>> ymlList) {
        List<Location> results = new ArrayList<>();

        for (Map<?, ?> location : ymlList) {
            Object worldObj = location.get("world");
            Object xObj = location.get("x");
            Object yObj = location.get("x");
            Object zObj = location.get("z");
            Object yawObj = location.get("yaw") != null ? location.get("yaw") : 0F;
            Object pitchObj = location.get("pitch") != null ? location.get("pitch") : 0F;

            if (List.of(worldObj, xObj, yObj, zObj).contains(null)) {
                throw new RuntimeException("Incomplete location! (Missing world, x, y, or z.)");
            }

            String worldName = (String) worldObj;
            double x = ((Number) xObj).doubleValue();
            double y = ((Number) yObj).doubleValue();
            double z = ((Number) zObj).doubleValue();

            float yaw = ObjectUtils.isValidNumber((String) yawObj) ? (float) ObjectUtils.parseAs(Float.class, (String) yawObj) : 0F;
            float pitch = ObjectUtils.isValidNumber((String) pitchObj) ? (float) ObjectUtils.parseAs(Float.class, (String) pitchObj) : 0F;

            World bukkitWorld = Bukkit.getWorld(worldName);

            Location loc = new Location(bukkitWorld, x, y, z, yaw, pitch);
            results.add(loc);
        }
        
        return results;
    }

    public static Location getLocationFromYML(List<Map<?, ?>> ymlList) {
        // Returns the first element of the list returned by getLocationsFromYML(List<Map<?, ?>> ymlList)\
        // Use if you are trying to get a single location (e.g. paste-location) from the config.

        List<Location> results = getLocationsFromYML(ymlList);
        
        if (results.isEmpty()) {
            return null;
        }

        return results.getFirst();
    }
    
    
}
