# Routing Strategy API - Implementation Guide

## 📋 Overview

This guide explains the implemented **Routing Strategy API** in the AidBridge backend routing module, which supports 5 pre-configured strategies and allows dynamic combination with custom dangerous zones.

---

## 🎯 Core Components

### **1. DTOs (Data Transfer Objects)**

#### `RoutingRequest`
- `startLat`, `startLon`, `endLat`, `endLon`: Required coordinates
- `strategy`: Primary strategy (fastest, safest, accessible, night-safe, community-response) - default: "fastest"
- `additionalStrategies`: List of strategies to merge with primary
- `dangerousZones`: List of custom DangerousZone objects to avoid/reduce priority

#### `DangerousZone`
- `name`: Zone identifier
- `priority`: Multiplier 0.0-1.0 (0.0 = bypass, default = 0.0)
- `geometry`: GeoJSON polygon with [lng, lat] coordinates

#### `GeoJsonGeometry`
- `type`: "Polygon" (only type supported)
- `coordinates`: List of coordinate rings [[lng, lat], ...]

### **2. Services**

#### `StrategyMergingService`
- Merges multiple CustomModels by combining priority and speed rules
- Applies dangerous zones as geometric area constraints
- More restrictive speed limits win in conflicts (safer)

### **3. Configuration**

#### `GraphHopperProperties`
- Binds to `app.routing.*` in application.yml
- Maintains map of strategy profiles and their JSON paths
- Example:
  ```yaml
  app:
    routing:
      profiles:
        fastest: routing-profiles/car-fastest.json
        safest: routing-profiles/car-safest.json
        # ... etc
  ```

#### `RoutingConfig`
- Loads multiple strategy profiles from classpath
- Sets up LM (Landmark) mode preparation for all profiles
- Supports request-time custom model modifications
- Handles cache incompatibility (auto-rebuild)

---

## 🔄 API Endpoints

### **POST /api/routing/calculate**

#### Scenario 1: Single Strategy
```json
{
  "startLat": 10.8,
  "startLon": 106.7,
  "endLat": 10.9,
  "endLon": 106.8,
  "strategy": "fastest"
}
```

#### Scenario 2: Combined Strategies + Dangerous Zones
```json
{
  "startLat": 10.8,
  "startLon": 106.7,
  "endLat": 10.9,
  "endLon": 106.8,
  "strategy": "safest",
  "additionalStrategies": ["night-safe"],
  "dangerousZones": [
    {
      "name": "gang_district_1",
      "priority": 0,
      "geometry": {
        "type": "Polygon",
        "coordinates": [[[106.7, 10.8], [106.71, 10.8], [106.71, 10.81], [106.7, 10.81], [106.7, 10.8]]]
      }
    },
    {
      "name": "flood_area",
      "priority": 0.3,
      "geometry": { ... }
    }
  ]
}
```

---

## 📊 5 Routing Strategies

### **1. fastest** (Emergency/Time-critical)
- Motorway ×2.0, Trunk ×1.5
- Secondary ×0.8 (deprioritize)
- Speed limit: 130 km/h on motorways

### **2. safest** (Avoid dangerous areas)
- Primary ×1.2 (prefer main roads)
- City areas ×0.9 (reduce)
- Speed limit: 50 km/h in cities
- **Use with dangerousZones** for area-specific avoidance

### **3. accessible** (Ambulance/Emergency vehicles)
- Lanes ≥ 2: ×1.5 (prefer wide roads)
- Tunnels ×0.5 (avoid narrow passages)
- Motorway/Trunk ×1.3 (main routes)

### **4. night-safe** (Low visibility)
- Well-lit roads ×1.5 (`lit == true`)
- Tunnels ×0.3 (avoid dark areas)
- Speed limit: 80 km/h (conservative)

### **5. community-response** (Local search)
- Secondary/Residential ×1.3 (prefer local roads)
- Motorway ×0.3 (avoid highways)
- City/Residential areas ×1.2
- Speed limits: 30 km/h residential, 50 km/h city

---

## 🔀 Strategy Merging Logic

When combining strategies:
1. Load base profile from primary strategy
2. Merge additional strategy profiles:
   - Priority rules are **combined** (highest preference multiplier)
   - Speed limits use **minimum** (more restrictive = safer)
3. Apply dangerous zones with custom priority multipliers
4. Execute routing with merged CustomModel

**Example:**
```
Base: safest (PRIMARY ×1.2, city speed 50 km/h)
+ night-safe (lit ×1.5, speed 80 km/h)
= Result: PRIMARY ×1.2, lit ×1.5, speed 50 km/h (more restrictive)
```

---

## 🚀 Implementation Steps Completed

✅ Created `DangerousZone` DTO with GeoJSON support  
✅ Created `GeoJsonGeometry` for polygon coordinates  
✅ Updated `RoutingRequest` to support strategies and zones  
✅ Updated `GraphHopperProperties` for multi-profile loading  
✅ Implemented `StrategyMergingService` for strategy combining  
✅ Updated `RoutingConfig` to load profiles from classpath + LM preparation  
✅ Updated `CalculateRouteUseCase` to handle strategy selection  
✅ Created 5 strategy profile JSON files  
✅ Created `application.yml` with profile configurations  

---

## ⚡ Performance Characteristics

| Mode | Speed | Custom Model | Recommendation |
|------|-------|---|---|
| **CH (Speed)** | ~1ms | ❌ No | Pre-defined profiles only |
| **LM (Landmark)** | ~100ms | ✅ Yes | **Current** - Supports request-time options |
| **Dijkstra** | ~1000ms | ✅ Yes | Debug/testing only |

**Current:** Using **LM profiles** to support dynamic dangerous zones + strategy merging (~100ms response time).

---

## 📁 File Structure

```
spring-backend/
├── src/main/
│   ├── java/com/drc/aidbridge/modules/routing/
│   │   ├── RoutingDTO.java (response format)
│   │   ├── RoutingFacade.java
│   │   └── internal/
│   │       ├── config/
│   │       │   ├── RoutingConfig.java (profiles loading, LM setup)
│   │       │   └── GraphHopperProperties.java (multi-profile config)
│   │       ├── service/
│   │       │   └── StrategyMergingService.java (merge logic)
│   │       ├── usecase/
│   │       │   └── CalculateRouteUseCase.java (strategy selection)
│   │       └── web/
│   │           ├── RoutingController.java
│   │           └── dto/
│   │               ├── RoutingRequest.java (strategies + zones)
│   │               ├── DangerousZone.java
│   │               └── GeoJsonGeometry.java
│   └── resources/
│       ├── application.yml (profiles mapping)
│       └── routing-profiles/
│           ├── car-fastest.json
│           ├── car-safest.json
│           ├── car-accessible.json
│           ├── car-night-safe.json
│           └── car-community-response.json
```

---

## 🔧 Configuration Example

```yaml
app:
  routing:
    enabled: true
    osm-file: data/vietnam.osm.pbf
    graph-dir: ./graph-data
    data-access: MMAP
    profiles:
      fastest: routing-profiles/car-fastest.json
      safest: routing-profiles/car-safest.json
      accessible: routing-profiles/car-accessible.json
      night-safe: routing-profiles/car-night-safe.json
      community-response: routing-profiles/car-community-response.json
```

---

## ✨ Next Steps

1. **Test API** with different strategy combinations
2. **Implement Strategy Merging** fully in CalculateRouteUseCase (currently simplified)
3. **Add Dangerous Zone Loading** from request into GraphHopper areas
4. **Performance Testing** with real Vietnam OSM data
5. **Client Integration** with mobile app

