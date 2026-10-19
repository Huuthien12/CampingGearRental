# Camping Gear Rental Management

**CampingGearRental** là ứng dụng quản lý cho thuê thiết bị cắm trại, được xây dựng phục vụ học phần **Mẫu thiết kế (Design Patterns)**. Hệ thống sử dụng **Java 21, Spring Boot MVC, Thymeleaf, JDBC và MySQL**, đồng thời minh họa bốn mẫu thiết kế GoF: **Singleton, Factory Method, State và Strategy**.

> **Phạm vi:** Dự án phục vụ học tập và trình diễn. Chức năng thanh toán là mô phỏng; hệ thống không tự đối soát giao dịch với ngân hàng và chưa được thiết kế để triển khai sản xuất.

## 1. Công nghệ sử dụng

- **Ngôn ngữ:** Java 21 (JDK 21 trở lên).
- **Build:** Maven 3.9+.
- **Web:** Spring Boot, Spring MVC, Thymeleaf, Bootstrap.
- **Cơ sở dữ liệu:** MySQL 8+, JDBC.
- **Kiểm thử:** JUnit 5 và các bài kiểm thử ứng dụng.
- **JavaFX:** mã prototype từ giai đoạn đầu được giữ lại; luồng trình diễn chính là ứng dụng web.

## 2. Chức năng chính

- Đăng ký, đăng nhập và đăng xuất; mật khẩu được lưu dưới dạng BCrypt.
- Phân quyền **ADMIN** và **CUSTOMER**; kiểm tra quyền truy cập và quyền sở hữu đơn thuê ở phía máy chủ.
- Quản lý khách hàng, danh mục thiết bị, hình ảnh và tồn kho.
- Tạo đơn thuê thiết bị tùy chọn hoặc theo gói **SOLO / COUPLE / FAMILY**.
- Quản lý vòng đời đơn thuê: **PENDING → CONFIRMED → RENTED → RETURNED**, cùng nhánh hủy khi hợp lệ.
- Tính tiền thuê và giảm giá theo số ngày; giữ đơn giá đã lưu tại thời điểm tạo đơn.
- Checkout với hai phương thức **CASH** và **BANK_TRANSFER**, trạng thái thanh toán **UNPAID / PAID**.
- Hiển thị **ảnh QR chuyển khoản cố định** khi có đủ cấu hình; chỉ ADMIN có quyền xác nhận đã nhận tiền.

## 3. Cài đặt và chạy ứng dụng

### 3.1. Yêu cầu

- JDK 21+ (`java -version`).
- Maven 3.9+ (`mvn -version`).
- MySQL 8+ đang hoạt động.
- Mã nguồn đã được clone về máy.

### 3.2. Chuẩn bị cơ sở dữ liệu

Tạo database MySQL tên `camping_gear_rental`.

**Nếu cài mới**, chạy theo thứ tự:

1. `database/schema.sql` — tạo cấu trúc bảng.
2. `database/seed.sql` — thêm dữ liệu mẫu.

**Nếu nâng cấp database cũ**, sao lưu dữ liệu trước. Với database v1, chạy lần lượt:

1. `database/migration-v2-roles.sql`
2. `database/migration-v3-payment-methods.sql`
3. `database/migration-v4-normalize-vietqr.sql`

Database đã ở v3 chỉ cần migration v4. Migration v2 giữ các tài khoản cũ ở vai trò `ADMIN` do phiên bản v1 chưa có tài khoản khách hàng; tài khoản cũ ngoài seed có thể cần cập nhật mật khẩu BCrypt trước khi đăng nhập.

> Không chạy lại script khởi tạo trên database đang chứa dữ liệu cần giữ nếu chưa kiểm tra và sao lưu.

### 3.3. Cấu hình môi trường

Ứng dụng đọc cấu hình MySQL từ các biến môi trường:

| Biến | Ý nghĩa |
| --- | --- |
| `CAMPING_DB_URL` | JDBC URL đến MySQL |
| `CAMPING_DB_USERNAME` | Tên tài khoản MySQL |
| `CAMPING_DB_PASSWORD` | Mật khẩu MySQL |

Ví dụ PowerShell (thay bằng thông tin trên máy của bạn):

```powershell
$env:CAMPING_DB_URL = 'jdbc:mysql://localhost:3306/camping_gear_rental'
$env:CAMPING_DB_USERNAME = 'your_mysql_user'
$env:CAMPING_DB_PASSWORD = 'your_mysql_password'
```

Có thể sử dụng các Java system properties tương ứng: `camping.db.url`, `camping.db.username`, `camping.db.password`.

**Không commit mật khẩu thật, thông tin truy cập cá nhân hoặc file BAT chứa bí mật lên GitHub.**

### 3.4. Chạy và kiểm thử

Tại thư mục gốc dự án:

```powershell
mvn clean test
mvn clean package
mvn spring-boot:run
```

Mở `http://localhost:8080/login` nếu ứng dụng dùng cổng mặc định. Kiểm tra log khởi động nếu đã thay đổi cổng.

Tài khoản seed phục vụ demo:

| Vai trò | Tên đăng nhập | Mật khẩu |
| --- | --- | --- |
| ADMIN | `admin` | `admin123` |
| CUSTOMER | `cus1` | `123456` |

Khách hàng cũng có thể tạo tài khoản tại `/register`. Đây chỉ là tài khoản mẫu, không dùng trong môi trường sản xuất.

### 3.5. Cấu hình chuyển khoản ngân hàng

Khai báo các biến môi trường sau khi muốn sử dụng phương thức `BANK_TRANSFER`:

```powershell
$env:CAMPING_PAYMENT_BANK_NAME = 'Your Bank'
$env:CAMPING_PAYMENT_ACCOUNT_NUMBER = 'Your Account Number'
$env:CAMPING_PAYMENT_ACCOUNT_NAME = 'Your Account Name'
```

Ảnh QR cố định được đặt tại:

`src/main/resources/static/images/payment/bank-qr.png`

Khi chọn chuyển khoản, checkout hiển thị ảnh QR cùng **tổng tiền của đơn** và **mã đơn thuê** để người dùng đối chiếu. Nếu thiếu ảnh QR, cấu hình tài khoản hoặc số tiền không hợp lệ, giao diện hiển thị cảnh báo và không cho CUSTOMER báo đã chuyển khoản. **Chỉ ADMIN có quyền chuyển trạng thái thanh toán thành PAID**; hệ thống không tự xác minh giao dịch ngân hàng.

> Ảnh QR trong thư mục `static` sẽ được phân phối cùng ứng dụng và có thể công khai khi push lên repository public. Chỉ sử dụng ảnh được phép công khai.

## 4. Phân quyền và bảo mật

- **ADMIN:** quản lý khách hàng, thiết bị, tồn kho, tất cả đơn thuê, chuyển trạng thái đơn và xác nhận thanh toán.
- **CUSTOMER:** xem thiết bị, tạo đơn thuê theo gói hoặc tùy chọn, xem giá và đơn của chính mình, yêu cầu hủy đơn khi quy tắc State cho phép.
- Kiểm tra quyền tại MVC interceptor và kiểm tra quyền sở hữu đơn trong controller; ẩn menu không thay thế việc phân quyền phía máy chủ.
- Các yêu cầu POST cần CSRF token theo phiên.
- Phiên đăng nhập được thay đổi sau khi đăng nhập và hết hạn sau 30 phút không hoạt động.
- Cookie dùng HttpOnly và SameSite=Lax; đặt `CAMPING_COOKIE_SECURE=true` khi chạy qua HTTPS.

## 5. Bốn mẫu thiết kế GoF

### 5.1. Singleton — Kết nối cơ sở dữ liệu

- **Lớp chính:** `singleton/DatabaseConnection.java`.
- **Ý tưởng:** constructor private, static holder và `getInstance()` cung cấp một đối tượng quản lý cấu hình/tạo kết nối.
- **Áp dụng:** JDBC repository lấy kết nối thông qua Singleton.
- **Lưu ý:** không phải tất cả request cùng chia sẻ một JDBC `Connection`; từng thao tác có thể mở và đóng kết nối riêng.

### 5.2. Factory Method — Tạo gói cắm trại

- **Creator:** `factory/CampingPackageCreator.java` với `createPackage()`.
- **Concrete Creators:** `SoloCampingPackageCreator`, `CoupleCampingPackageCreator`, `FamilyCampingPackageCreator`.
- **Product:** `CampingPackage` và các `CampingPackageItem`.
- **Bộ chọn:** `CampingPackageFactory` chọn Creator; logic tạo gói nằm trong Concrete Creator.
- **Áp dụng:** tạo và hiển thị gói SOLO, COUPLE, FAMILY trong luồng thuê.

Đây là **GoF Factory Method**, thay cho cách Simple Factory ban đầu dùng một phương thức `switch` trực tiếp tạo mọi gói. Dữ liệu demo cần có thiết bị `EQ001`–`EQ005` với tồn kho phù hợp.

### 5.3. State — Vòng đời đơn thuê

- **Interface:** `state/RentalState.java`.
- **Concrete States:** `PendingState`, `ConfirmedState`, `RentedState`, `ReturnedState`, `CancelledState`.
- **Context:** `model/RentalOrder.java`.
- **Áp dụng:** kiểm soát các thao tác `confirm`, `rent`, `returnEquipment`, `cancel` theo trạng thái.
- **Trách nhiệm:** `RentalService` xử lý giao dịch và cập nhật tồn kho; State kiểm soát chuyển trạng thái.

### 5.4. Strategy — Tính giá thuê

- **Interface:** `strategy/PricingStrategy.java`.
- **Concrete Strategies:** `NormalPricingStrategy`, `LongTermPricingStrategy`.
- **Context/selector:** `service/PricingService.java`.
- **Quy tắc demo:** thuê dưới 5 ngày không giảm giá; thuê từ **5 ngày trở lên giảm 10%**.
- **Giá lịch sử:** checkout dùng `RentalDetail.unitPrice` đã lưu khi tạo đơn, không lấy lại giá thiết bị hiện tại.

## 6. Quy trình demo đề xuất

1. **Singleton:** đăng nhập hoặc mở danh sách thiết bị; giải thích repository lấy kết nối qua `DatabaseConnection.getInstance()`.
2. **Factory Method:** vào `/rentals/new`, chọn gói SOLO, COUPLE hoặc FAMILY và tạo đơn nháp.
3. **Strategy:** so sánh đơn thuê dưới 5 ngày với đơn thuê từ 5 ngày trở lên trong checkout.
4. **State:** thực hiện `PENDING → CONFIRMED → RENTED → RETURNED`, quan sát tồn kho thay đổi.
5. **Phân quyền:** đăng nhập ADMIN và CUSTOMER, kiểm tra các màn hình và thao tác được phép.
6. **Thanh toán:** thử CASH và BANK_TRANSFER; CUSTOMER báo chuyển khoản khi đủ điều kiện, ADMIN xác nhận PAID.
7. **Kiểm tra lỗi:** thử thao tác không hợp lệ theo trạng thái và truy cập đơn của khách hàng khác.

> Trước khi demo, kiểm tra MySQL, tài khoản seed, các thiết bị `EQ001`–`EQ005`, tồn kho, ảnh QR và cấu hình ngân hàng nếu trình diễn chuyển khoản.

## 7. Cấu trúc dự án

```text
CampingGearRental/
├── database/
│   ├── schema.sql
│   ├── seed.sql
│   ├── migration-v2-roles.sql
│   ├── migration-v3-payment-methods.sql
│   └── migration-v4-normalize-vietqr.sql
├── docs/
├── src/
│   ├── main/
│   │   ├── java/com/campinggearrental/
│   │   │   ├── factory/       # Factory Method
│   │   │   ├── singleton/     # Singleton
│   │   │   ├── state/         # State
│   │   │   ├── strategy/      # Strategy
│   │   │   ├── model/         # Đối tượng nghiệp vụ
│   │   │   ├── repository/    # Truy cập dữ liệu
│   │   │   ├── service/       # Xử lý nghiệp vụ
│   │   │   └── web/           # Web controllers
│   │   └── resources/        # Cấu hình, templates, CSS, JS, hình ảnh
│   └── test/java/            # Kiểm thử
├── pom.xml
└── README.md
```

Luồng web tổng quát:

```text
Browser → Thymeleaf View → Spring MVC Controller → Service → Repository/JDBC → MySQL
                                           ├── Factory Method
                                           ├── State
                                           └── Strategy
                      JDBC infrastructure → Singleton
```

JavaFX được giữ lại như prototype của giai đoạn đầu.

## 8. Phân công nhóm

| Thành viên | Phần phụ trách | Mẫu GoF |
| --- | --- | --- |
| Nguyễn Minh Long (A) | Tài khoản, khách hàng, kết nối cơ sở dữ liệu | Singleton |
| Hoàng Bình Quân (B) | Thiết bị, tồn kho, gói cắm trại | Factory Method |
| Nguyễn Hồng Phúc Thọ (C) | Đơn thuê, nhận/trả thiết bị | State |
| Lương Hữu Thiện (D) | Tính giá, checkout, thanh toán demo | Strategy |

## 9. Giới hạn và lưu ý

- Đây là ứng dụng minh họa học phần, không tích hợp cổng thanh toán hoặc đối soát ngân hàng thực tế.
- Không sử dụng tài khoản seed, mật khẩu demo hoặc QR cá nhân không muốn công khai trong môi trường sản xuất.
- Các gói cắm trại phụ thuộc thiết bị seed cố định; cần chuẩn bị dữ liệu trước khi demo.
- Kiểm thử tự động không thay thế hoàn toàn kiểm thử end-to-end với MySQL thật.
- Khi thay đổi schema hoặc nghiệp vụ, cần kiểm tra lại bốn mẫu GoF và chạy kiểm thử hồi quy.
