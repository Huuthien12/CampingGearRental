# Camping Gear Rental Management

**CampingGearRental** là ứng dụng quản lý cho thuê thiết bị cắm trại, được xây dựng phục vụ học phần **Mẫu thiết kế (Design Patterns)**. Hệ thống minh họa cách áp dụng **4 mẫu thiết kế thuộc bộ GoF** vào một quy trình nghiệp vụ thống nhất: **Singleton, Factory Method, State và Strategy**.

> **Phạm vi:** Dự án phục vụ học tập và trình diễn, chưa được thiết kế để triển khai sản xuất hoặc xử lý thanh toán trực tuyến thực tế.

## 1. Công nghệ sử dụng

- **Ngôn ngữ:** Java 21 (yêu cầu JDK 21 trở lên).
- **Quản lý dự án/build:** Maven 3.9+.
- **Ứng dụng web:** Spring Boot, Spring MVC và Thymeleaf.
- **Cơ sở dữ liệu:** MySQL 8+, truy cập qua JDBC.
- **Kiểm thử:** JUnit 5 và các bài kiểm thử ứng dụng.
- **JavaFX:** mã/prototype từ giai đoạn đầu có thể vẫn tồn tại; **luồng trình diễn chính là ứng dụng web**, không phải `mvn javafx:run`.

## 2. Chức năng chính

- Đăng nhập/đăng xuất bằng tài khoản demo.
- Quản lý và tra cứu khách hàng.
- Quản lý danh mục thiết bị và tồn kho.
- Tạo đơn thuê theo thiết bị tùy chọn hoặc theo gói cắm trại **SOLO / COUPLE / FAMILY**.
- Quản lý vòng đời đơn thuê: **PENDING → CONFIRMED → RENTED → RETURNED**, cùng nhánh hủy hợp lệ.
- Tính tiền thuê theo số ngày và chiến lược giảm giá.
- Xem thông tin checkout và ghi nhận trạng thái thanh toán **UNPAID / PAID**.

## 3. Cài đặt và chạy ứng dụng

### 3.1. Yêu cầu

- JDK 21+; kiểm tra bằng `java -version`.
- Maven 3.9+; kiểm tra bằng `mvn -version`.
- MySQL 8+ đang hoạt động.
- Đã tải hoặc clone mã nguồn dự án.

### 3.2. Chuẩn bị cơ sở dữ liệu

Tạo cơ sở dữ liệu MySQL tên `camping_gear_rental`, sau đó thực thi các script **theo thứ tự**:

1. `database/schema.sql` — tạo cấu trúc bảng.
2. `database/seed.sql` — thêm dữ liệu mẫu.

**Chỉ chạy các script khởi tạo trên cơ sở dữ liệu demo mới hoặc môi trường được phép khởi tạo.** Không chạy lại trên cơ sở dữ liệu đang chứa dữ liệu cần giữ nếu chưa kiểm tra nội dung script và sao lưu.

Cấu hình kết nối thông qua các biến môi trường:

| Biến | Ý nghĩa |
| --- | --- |
| `CAMPING_DB_URL` | JDBC URL đến MySQL |
| `CAMPING_DB_USERNAME` | Tên tài khoản MySQL |
| `CAMPING_DB_PASSWORD` | Mật khẩu MySQL |

Ví dụ PowerShell (thay các giá trị bằng cấu hình trên máy của bạn):

```powershell
$env:CAMPING_DB_URL = 'jdbc:mysql://localhost:3306/camping_gear_rental'
$env:CAMPING_DB_USERNAME = 'your_mysql_user'
$env:CAMPING_DB_PASSWORD = 'your_mysql_password'
```

Mã nguồn trước đây cũng hỗ trợ các Java system properties tương ứng: `camping.db.url`, `camping.db.username` và `camping.db.password`.

> Không lưu mật khẩu thật trong README, mã nguồn hoặc commit Git. Không sử dụng tài khoản MySQL đặc quyền cao cho môi trường triển khai thực tế.

### 3.3. Kiểm thử và đóng gói

Chạy tại thư mục gốc dự án:

```powershell
mvn clean test
mvn clean package
```

**Kết quả tham khảo:** sau đợt nâng cấp Factory Method, Codex báo cáo **213 tests PASS**, không có failure/error. Đây là kết quả của lần chạy đã báo cáo, không phải cam kết mọi môi trường đều có cùng kết quả. Các bài kiểm thử tích hợp MySQL yêu cầu môi trường riêng có thể không nằm trong lệnh kiểm thử mặc định.

### 3.4. Chạy giao diện web

Nếu dự án sử dụng cấu hình Spring Boot Maven Plugin thông thường, chạy:

```powershell
mvn spring-boot:run
```

Mở địa chỉ web được in trong log khởi động (thường là `http://localhost:8080` **nếu không đổi cổng**). Nếu lệnh không khả dụng, kiểm tra cấu hình plugin và lớp khởi động Spring Boot trong `pom.xml`/mã nguồn của phiên bản hiện tại.

**Tài khoản demo từ dữ liệu seed gốc:** `admin` / `admin123`. Chỉ sử dụng trong môi trường demo, thay đổi cơ chế lưu mật khẩu và tài khoản trước khi dùng với dữ liệu thật.

## 4. Bốn mẫu thiết kế GoF

### 4.1. Singleton — Kết nối cơ sở dữ liệu

- **Lớp chính:** `singleton/DatabaseConnection.java`.
- **Ý tưởng:** dùng constructor private, static holder và `getInstance()` để cung cấp một đối tượng quản lý cấu hình/tạo kết nối.
- **Áp dụng:** các JDBC repository sử dụng đối tượng này để lấy kết nối khi thao tác với MySQL.
- **Lưu ý:** Singleton **không có nghĩa** toàn bộ request chia sẻ cùng một JDBC `Connection`; mỗi thao tác có thể mở/đóng kết nối riêng.

### 4.2. Factory Method — Tạo gói cắm trại

- **Creator:** `factory/CampingPackageCreator.java` với phương thức `createPackage()`.
- **Concrete Creators:** `SoloCampingPackageCreator`, `CoupleCampingPackageCreator`, `FamilyCampingPackageCreator`.
- **Product:** `CampingPackage`, gồm các `CampingPackageItem`.
- **Bộ chọn:** `CampingPackageFactory` chọn Creator phù hợp; logic tạo thành phần gói nằm trong các Concrete Creator.
- **Áp dụng:** tạo/hiển thị gói cắm trại trong luồng thuê qua web và tạo đơn nháp từ gói.

Đây là **GoF Factory Method**, thay cho cách Simple Factory ban đầu dùng một phương thức `switch` trực tiếp tạo mọi gói.

**Lưu ý dữ liệu demo:** các gói vẫn tham chiếu thiết bị seed `EQ001`–`EQ005`. Trước khi demo, cần xác nhận các thiết bị cần thiết tồn tại, đang khả dụng và có đủ tồn kho.

### 4.3. State — Vòng đời đơn thuê

- **State interface:** `state/RentalState.java`.
- **Concrete States:** `PendingState`, `ConfirmedState`, `RentedState`, `ReturnedState`, `CancelledState`.
- **Context:** `model/RentalOrder.java`.
- **Áp dụng:** mỗi trạng thái quy định thao tác `confirm`, `rent`, `returnEquipment` hoặc `cancel` có hợp lệ hay không.
- **Phân chia trách nhiệm:** `RentalService` xử lý giao dịch và cập nhật tồn kho; State kiểm soát chuyển trạng thái.

### 4.4. Strategy — Tính giá thuê

- **Strategy interface:** `strategy/PricingStrategy.java`.
- **Concrete Strategies:** `NormalPricingStrategy`, `LongTermPricingStrategy`.
- **Context/selector:** `service/PricingService.java`.
- **Áp dụng:** thuê thông thường không giảm giá; thuê **từ 5 ngày trở lên** áp dụng chiến lược giảm **10%** theo quy tắc demo.
- **Đảm bảo giá lịch sử:** checkout sử dụng `RentalDetail.unitPrice` được lưu tại thời điểm tạo đơn thay vì lấy lại giá thiết bị hiện tại.

## 5. Quy trình demo đề xuất

1. **Singleton:** đăng nhập hoặc mở danh sách thiết bị; giải thích repository lấy kết nối qua `DatabaseConnection.getInstance()`.
2. **Factory Method:** mở `/rentals/new`, chọn gói **SOLO**, **COUPLE** hoặc **FAMILY**; xem thành phần gói và tạo đơn nháp.
3. **Strategy:** tạo đơn với thời hạn dưới 5 ngày và từ 5 ngày trở lên; mở checkout để so sánh tiền giảm giá và tổng tiền.
4. **State:** xác nhận đơn `PENDING → CONFIRMED`, bàn giao `CONFIRMED → RENTED`, trả thiết bị `RENTED → RETURNED`; quan sát tồn kho thay đổi phù hợp.
5. **Kiểm tra lỗi hợp lệ:** thử thao tác không được phép theo trạng thái; kiểm tra thông báo lỗi và tính toàn vẹn dữ liệu.

> **Checklist trước demo:** MySQL kết nối được; tài khoản demo đăng nhập được; khách hàng mẫu tồn tại; các thiết bị gói `EQ001`–`EQ005` tồn tại và đủ tồn kho; ứng dụng khởi động không lỗi. Không chạy lại seed trên dữ liệu đang sử dụng mà chưa kiểm tra.

## 6. Cấu trúc dự án (các thành phần chính)

```text
CampingGearRental/
├── database/
│   ├── schema.sql
│   └── seed.sql
├── docs/
├── src/
│   ├── main/
│   │   ├── java/com/campinggearrental/
│   │   │   ├── factory/        # Factory Method
│   │   │   ├── singleton/      # Singleton
│   │   │   ├── state/          # State
│   │   │   ├── strategy/       # Strategy
│   │   │   ├── model/          # Đối tượng nghiệp vụ
│   │   │   ├── repository/     # Truy cập dữ liệu
│   │   │   ├── service/        # Xử lý nghiệp vụ
│   │   │   └── web/            # Web controllers
│   │   └── resources/         # Cấu hình, templates và tài nguyên
│   └── test/java/             # Kiểm thử
├── pom.xml
└── README.md
```

Luồng web tổng quát:

```text
Browser → Spring MVC Controller → Service → Repository/JDBC → MySQL
                                    ├── Factory Method
                                    ├── State
                                    └── Strategy
                 JDBC infrastructure → Singleton
```

## 7. Phân công nhóm

| Thành viên | Phần phụ trách | Mẫu GoF |
| --- | --- | --- |
| Nguyễn Minh Long (A) | Tài khoản, khách hàng, kết nối cơ sở dữ liệu | Singleton |
| Hoàng Bình Quân (B) | Thiết bị, tồn kho, gói cắm trại | Factory Method |
| Nguyễn Hồng Phúc Thọ (C) | Đơn thuê, nhận/trả thiết bị | State |
| Lương Hữu Thiện (D) | Tính giá, checkout, thanh toán demo | Strategy |

## 8. Giới hạn và lưu ý

- Đây là ứng dụng minh họa học phần; không có tích hợp cổng thanh toán thật.
- Tài khoản seed và cơ chế xác thực trong prototype không phù hợp môi trường sản xuất nếu vẫn lưu/so khớp mật khẩu dạng văn bản thuần.
- Các gói cắm trại phụ thuộc mã thiết bị seed cố định; cần chuẩn bị dữ liệu trước khi demo.
- Kiểm thử đơn vị và kiểm thử workflow không thay thế hoàn toàn kiểm thử end-to-end với MySQL thật.
- Khi bổ sung tính năng hoặc thay đổi schema, cần kiểm tra lại các mẫu GoF và chạy hồi quy.
