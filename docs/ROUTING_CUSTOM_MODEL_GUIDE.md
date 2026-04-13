# GraphHopper Custom Model & Multiple Routing Strategies

## 📌 Tóm Tắt

GraphHopper example chứa **3 tính năng chính**:

| Tính Năng | Mục Đích | Liên Quan? |
|-----------|---------|----------|
| **Basic Routing** | Route đơn giản | ❌ Không cần |
| **Alternative Route** | Tìm nhiều đường | ❌ Không cần |
| **Custom Model** | **Tùy biến logic routing** | ✅ **Đây là cái bạn cần** |

---

## 🎯 Custom Model Là Gì?

Custom Model cho phép **thay đổi cách GraphHopper tính toán route mà không rebuild cache**.

### Cách Hoạt Động

```
1. GraphHopper xây dựng graph từ OSM (lần đầu 10-20 phút)
   └─> Graph được lưu vào cache

2. Khi routing, GraphHopper apply Custom Model để:
   - Nhân ưu tiên road_class (MOTORWAY = nhanh hơn)
   - Giới hạn tốc độ khu vực
   - Tránh hoặc ưu tiên vùng nào đó
   - Chỉnh sửa priority ngay tại request time (LM mode)
```

### Ví Dụ Từ Example Code

**Cách 1: Load từ file (profile cố định)**
```java
new Profile("car_custom")
    .setCustomModel(GHUtility.loadCustomModelFromJar("car.json"))
```

**Cách 2: Tạo Custom Model tại request time (linh hoạt)**
```java
CustomModel model = new CustomModel();
model.addToPriority(If("road_class == SECONDARY", MULTIPLY, "0.5"));
model.addToSpeed(If("true", LIMIT, "30"));

req.setCustomModel(model);  // ✓ Apply vào request này thôi
```

---

## 💡 Làm Sao Áp Dụng Vào AidBridge?

### **Scenario Của Bạn (App Cứu Trợ SOS)**

Bạn muốn client chọn loại route phù hợp tình huống:

1. **fastest** (Route Nhanh Nhất)
   - Prioritize `road_class == MOTORWAY` (×2.0)
   - Tránh traffic jam → reduce secondary roads
   - **Dùng khi:** Cứu trợ khẩn cấp, thời gian là quan trọng

2. **safest** (Route An Toàn)
   - Avoid dangerous zones (geometric areas)
   - Reduce speed in `urban_density == CITY`
   - Prefer `road_class == PRIMARY`
   - **Dùng khi:** Vùng nguy hiểm, ban đêm, không quen đường

3. **accessible** (Route Cho Ambulance)
   - Bypass height/width restrictions (`max_height`, `max_width` ignored)
   - Prioritize `road_environment != TUNNEL`
   - Prefer roads với trên 2 lanes (`lanes >= 2`)
   - **Dùng khi:** Xe cứu thương, xe chỉ huy, cần qua nhiều loại đường

4. **night-safe** (Route Ban Đêm)
   - Prioritize `lit == YES` (prefer well-lit roads)
   - Avoid `road_environment == TUNNEL`
   - Reduce speed limit (×0.8)
   - **Dùng khi:** Hoạt động cứu trợ ban đêm, visibility thấp

5. **community-response** (Route Cộng Đồng)
   - Prioritize `road_class == SECONDARY` + `road_class == RESIDENTIAL`
   - Avoid motorways (×0.3)
   - Prefer `urban_density == RESIDENTIAL` hoặc `CITY`
   - **Dùng khi:** Phản ứng cộng đồng, tìm kiếm người bộ hành, an toàn cho đối tượng bộ hành

### **Giải Pháp: Hybrid Approach**

**1️⃣ Có sẵn 5 predefined profiles (fixed per app version)**

```yaml
# application.yml
app:
  routing:
    profiles:
      fastest: "routing-profiles/car-fastest.json"
      safest: "routing-profiles/car-safest.json"
      accessible: "routing-profiles/car-accessible.json"
      night-safe: "routing-profiles/car-night-safe.json"
      community-response: "routing-profiles/car-community-response.json"
```

Load lúc startup, không thay đổi được.

**2️⃣ Cho phép client custom tại request time (linh hoạt)**

```json
POST /api/routing/calculate
{
  "startLat": 10.8,
  "startLon": 106.7,
  "endLat": 10.9,
  "endLon": 106.8,
  
  "routeStrategy": "fastest",        // Use predefined profile
  
  "customPriority": {                // OR override với tùy chỉnh
    "if": "road_class == SECONDARY",
    "multiply_by": 0.5
  },
  "customSpeedLimit": {
    "limit_to": 50
  }
}
```

---

## 🔧 Code Implementation

### **Step 1: RoutingRequest DTO**

```java
@Data
@Builder
public class RoutingRequest {
    @NotNull
    private Double startLat;
    @NotNull
    private Double startLon;
    @NotNull
    private Double endLat;
    @NotNull
    private Double endLon;
    
    // Strategy: "fastest" | "safest" | "accessible" | "night-safe" | "community-response"
    private String routeStrategy;
    
    // Optional: Custom priority rule tại request time
    private Map<String, Object> customPriority;
    
    // Optional: Custom speed limit
    private Integer customSpeedLimit;
}
```

### **Step 2: Update CalculateRouteUseCase**

```java
@Component
@RequiredArgsConstructor
public class CalculateRouteUseCase {

    private final GraphHopper graphHopper;

    public RoutingDTO execute(RoutingRequest request) {
        
        // 1. Load base profile từ config
        String strategy = request.getRouteStrategy() != null 
            ? request.getRouteStrategy() 
            : "fastest";
        
        GHRequest ghRequest = new GHRequest(
            request.startLat(), request.startLon(),
            request.endLat(), request.endLon()
        );
        ghRequest.setProfile(strategy);
        ghRequest.setLocale(Locale.forLanguageTag("vi"));
        
        // 2. Optional: Apply request-time custom overrides
        if (request.getCustomPriority() != null || request.getCustomSpeedLimit() != null) {
            CustomModel customModel = buildCustomModel(
                request.getCustomPriority(),
                request.getCustomSpeedLimit()
            );
            ghRequest.setCustomModel(customModel);
        }
        
        // 3. Route
        GHResponse response = graphHopper.route(ghRequest);
        
        if (response.hasErrors()) {
            throw new IllegalStateException("Routing failed: " + response.getErrors().get(0).getMessage());
        }
        
        // ... extract response ...
    }
    
    private CustomModel buildCustomModel(
        Map<String, Object> customPriority,
        Integer customSpeedLimit
    ) {
        CustomModel model = new CustomModel();
        
        if (customPriority != null) {
            // Ví dụ: {"if": "road_class == SECONDARY", "multiply_by": 0.5}
            String condition = (String) customPriority.get("if");
            Double multiplier = ((Number) customPriority.get("multiply_by")).doubleValue();
            
            model.addToPriority(
                If(condition, MULTIPLY, String.valueOf(multiplier))
            );
        }
        
        if (customSpeedLimit != null) {
            // Limit to specified speed
            model.addToSpeed(
                If("true", LIMIT, String.valueOf(customSpeedLimit))
            );
        }
        
        return model;
    }
}
```

### **Step 3: Pre-defined Profiles (Config)**

**car-fastest.json** - Nhanh nhất (Cứu trợ khẩn cấp)
```json
{
  "version": "1",
  "priority": [
    {
      "if": "road_class == MOTORWAY",
      "multiply_by": 2.0
    },
    {
      "if": "road_class == TRUNK",
      "multiply_by": 1.5
    },
    {
      "if": "road_class == SECONDARY",
      "multiply_by": 0.8
    }
  ],
  "speed": [
    {
      "if": "road_class == MOTORWAY",
      "limit_to": 130
    }
  ]
}
```

**car-safest.json** - An toàn (Tránh khu nguy hiểm)
```json
{
  "version": "1",
  "priority": [
    {
      "if": "in_dangerous_zone",
      "multiply_by": 0
    },
    {
      "if": "road_class == PRIMARY",
      "multiply_by": 1.2
    },
    {
      "if": "urban_density == CITY",
      "multiply_by": 0.9
    }
  ],
  "speed": [
    {
      "if": "urban_density == CITY",
      "limit_to": 50
    }
  ],
  "areas": {
    "type": "FeatureCollection",
    "features": [
      {
        "type": "Feature",
        "id": "dangerous_zone",
        "geometry": {
          "type": "Polygon",
          "coordinates": [[[14.3, 51.75], [14.35, 51.75], [14.34, 51.77], [14.33, 51.77], [14.3, 51.75]]]
        }
      }
    ]
  }
}
```

**car-accessible.json** - Ambulance/Emergency (Không giới hạn chiều cao, ưu tiên đường rộng)
```json
{
  "version": "1",
  "priority": [
    {
      "if": "lanes >= 2",
      "multiply_by": 1.5
    },
    {
      "if": "road_environment == TUNNEL",
      "multiply_by": 0.5
    },
    {
      "if": "road_class == MOTORWAY || road_class == TRUNK",
      "multiply_by": 1.3
    }
  ],
  "speed": []
}
```

**car-night-safe.json** - Ban đêm (Ưu tiên đèn chiếu sáng, tránh tunnel)
```json
{
  "version": "1",
  "priority": [
    {
      "if": "lit == true",
      "multiply_by": 1.5
    },
    {
      "if": "road_environment == TUNNEL",
      "multiply_by": 0.3
    },
    {
      "if": "road_class == PRIMARY || road_class == SECONDARY",
      "multiply_by": 1.1
    }
  ],
  "speed": [
    {
      "if": "true",
      "limit_to": 80
    }
  ]
}
```

**car-community-response.json** - Phản ứng cộng đồng (Ưu tiên đường dân cư, tránh cao tốc)
```json
{
  "version": "1",
  "priority": [
    {
      "if": "road_class == SECONDARY || road_class == RESIDENTIAL",
      "multiply_by": 1.3
    },
    {
      "if": "urban_density == RESIDENTIAL || urban_density == CITY",
      "multiply_by": 1.2
    },
    {
      "if": "road_class == MOTORWAY",
      "multiply_by": 0.3
    }
  ],
  "speed": [
    {
      "if": "road_class == RESIDENTIAL",
      "limit_to": 30
    },
    {
      "if": "urban_density == CITY",
      "limit_to": 50
    }
  ]
}
```

---

## 🎮 Workflow: Client Sử Dụng

### **Scenario 1: Route Nhanh Nhất (Cứu Trợ Khẩn Cấp)**
```json
POST /api/routing/calculate
{
  "startLat": 10.8,
  "startLon": 106.7,
  "endLat": 10.9,
  "endLon": 106.8,
  "routeStrategy": "fastest"
}
// Result: Ưu tiên motorway, bypass secondary roads
```

### **Scenario 2: Route An Toàn (Tránh Vùng Nguy Hiểm)**
```json
POST /api/routing/calculate
{
  "startLat": 10.8,
  "startLon": 106.7,
  "endLat": 10.9,
  "endLon": 106.8,
  "routeStrategy": "safest"
}
// Result: Avoid dangerous_zone, reduce speed in city areas
```

### **Scenario 3: Route Ambulance (Không Giới Hạn Chiều Cao/Rộng)**
```json
POST /api/routing/calculate
{
  "startLat": 10.8,
  "startLon": 106.7,
  "endLat": 10.9,
  "endLon": 106.8,
  "routeStrategy": "accessible"
}
// Result: Ưu tiên đường với ≥2 lanes, tránh tunnels
```

### **Scenario 4: Route Ban Đêm (Ưu Tiên Đèn Chiếu Sáng)**
```json
POST /api/routing/calculate
{
  "startLat": 10.8,
  "startLon": 106.7,
  "endLat": 10.9,
  "endLon": 106.8,
  "routeStrategy": "night-safe"
}
// Result: Prefer well-lit roads, avoid tunnels, reduce speed
```

### **Scenario 5: Route Cộng Đồng (Phản Ứng Địa Phương)**
```json
POST /api/routing/calculate
{
  "startLat": 10.8,
  "startLon": 106.7,
  "endLat": 10.9,
  "endLon": 106.8,
  "routeStrategy": "community-response"
}
// Result: Prefer secondary/residential roads, avoid motorways
```

### **Scenario 6: Custom Override**
```json
POST /api/routing/calculate
{
  "startLat": 10.8,
  "startLon": 106.7,
  "endLat": 10.9,
  "endLon": 106.8,
  "routeStrategy": "fastest",
  "customSpeedLimit": 50,
  "customPriority": {
    "if": "road_class == SECONDARY",
    "multiply_by": 0.5
  }
}
// Result: Load "fastest" profile + override speed + priority
```

---

## ⚡ Performance Notes

### **Speed Mode (CH - Contraction Hierarchies)**
- ✅ Rất nhanh (~1ms)
- ❌ Không hỗ trợ request-time custom model
- **Dùng cho:** Pre-defined profiles chỉ

### **Hybrid Mode (LM - Landmarks)**
- ✅ Nhanh (~100ms)
- ✅ **Hỗ trợ request-time custom model**
- **Dùng cho:** Cho phép client override tại request

### **Flexible Mode (Dijkstra)**
- ✅ Support mọi feature
- ❌ Chậm (~1000ms+)
- **Dùng cho:** Development/debugging

**Khuyến nghị:** Dùng **LM profiles** cho profiles bạn support.

```java
// RoutingConfig.java
hopper.getLMPreparationHandler()
    .setLMProfiles(
        new LMProfile("fastest"),
        new LMProfile("safest"),
        new LMProfile("accessible"),
        new LMProfile("night-safe"),
        new LMProfile("community-response")
    );
```

---

## 📊 So Sánh: GraphHopper API vs Giải Pháp Của Bạn

| Feature | GraphHopper API | Giải Pháp Bạn |
|---------|-----------------|--------------|
| Request-time custom model | ✅ Yes | ✅ Yes |
| Multiple strategies | ✅ Yes | ✅ Yes |
| Depend on external service | ✅ Yes | ❌ No (Local) |
| Performance | Phụ Network | ⚡ Local |
| Data privacy | ❌ Không | ✅ Yes |
| Cost | $ Per API call | $0 |

---

## 🚀 Next Steps

1. **Tạo 5 custom model files** → `resources/routing-profiles/`
   - `car-fastest.json`
   - `car-safest.json`
   - `car-accessible.json`
   - `car-night-safe.json`
   - `car-community-response.json`

2. **Update GraphHopperProperties** → Thêm 5 profiles vào map
3. **Update RoutingConfig** → Load multiple profiles + LM preparation
4. **Update CalculateRouteUseCase** → Support request-time custom model
5. **Update RoutingRequest** → Thêm `routeStrategy` + custom fields
6. **Test** → Gọi API với các strategies khác nhau

