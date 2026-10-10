# KẾ HOẠCH & SRS CHI TIẾT --- CAMPING GEAR RENTAL WEB v2.0

**Đề tài:** Xây dựng hệ thống quản lý cho thuê thiết bị cắm trại áp dụng
các mẫu thiết kế bằng Java\
**Phiên bản kế hoạch:** Web v2.0\
**Nền tảng kế thừa:** CampingGearRental v1.0.0 (JavaFX + Maven + MySQL,
77 automated tests PASS)\
**Mục tiêu v2.0:** Chuyển lớp giao diện sang Web bằng Spring Boot +
Thymeleaf + Bootstrap, giữ và tái sử dụng tối đa domain/business logic
đã kiểm thử.

------------------------------------------------------------------------

## 1. Thành viên và trách nhiệm chính

  -----------------------------------------------------------------------
  Mã                Thành viên        Phụ trách chính   Design Pattern
  ----------------- ----------------- ----------------- -----------------
  A                 Nguyễn Minh Long  Account,          Singleton
                                      Customer,         
                                      Dashboard, tích   
                                      hợp nền tảng Web  

  B                 Hoàng Bình Quân   Equipment,        Factory
                                      Category,         
                                      Inventory,        
                                      Camping Package   

  C                 Nguyễn Hồng Phúc  Rental Order,     State
                    Thọ               Rent/Return,      
                                      lifecycle         

  D                 Lương Hữu Thiện   Nền tảng          Strategy
                                      migration,        
                                      Pricing,          
                                      Checkout,         
                                      Payment,          
                                      integration       
  -----------------------------------------------------------------------

> Nguyên tắc: mỗi thành viên chịu trách nhiệm module của mình nhưng
> không tự ý sửa business rule/module của người khác. Mọi thay đổi liên
> module phải qua PR và review.

------------------------------------------------------------------------

# 2. Mục tiêu sản phẩm

Web v2.0 phải tạo được một hệ thống quản lý cho thuê thiết bị cắm trại
có thể trình diễn trọn vẹn:

**Đăng nhập → Dashboard → Khách hàng → Thiết bị → Tạo đơn thuê → Xác
nhận → Giao thiết bị → Tính giá/Thanh toán → Trả thiết bị → Khôi phục
tồn kho → Xem lịch sử.**

Hệ thống phải thể hiện rõ bốn mẫu thiết kế: - **Singleton:** quản lý tài
nguyên/cấu hình dùng chung phù hợp với kiến trúc ứng dụng. -
**Factory:** tạo các gói thiết bị cắm trại. - **State:** quản lý vòng
đời đơn thuê. - **Strategy:** thay đổi chiến lược tính giá.

------------------------------------------------------------------------

# 3. Phạm vi

## 3.1 Trong phạm vi

-   Web application Java.
-   Spring Boot + Spring MVC.
-   Thymeleaf.
-   Bootstrap.
-   MySQL.
-   Maven.
-   Login nhân viên.
-   Dashboard.
-   CRUD khách hàng.
-   CRUD danh mục/thiết bị.
-   Theo dõi tồn kho.
-   Tạo và quản lý đơn thuê.
-   State lifecycle.
-   Pricing Strategy.
-   Checkout/payment trạng thái.
-   Camping Package Factory.
-   Validation và thông báo lỗi.
-   Unit/integration tests.
-   Demo workflow hoàn chỉnh.

## 3.2 Ngoài phạm vi

-   Đăng ký tài khoản công khai.
-   Phân quyền phức tạp.
-   Thanh toán online thật.
-   SMS/email thật.
-   Vận chuyển/logistics thật.
-   Multi-store.
-   Mobile app.
-   Microservices.
-   React/Vue SPA.
-   Cloud deployment bắt buộc.

Các mục ngoài phạm vi chỉ thực hiện nếu toàn bộ SRS bắt buộc đã PASS.

------------------------------------------------------------------------

# 4. Kiến trúc mục tiêu

``` text
Browser
   |
HTTP
   v
Spring MVC Controller
   |
   v
Service / Business Layer
   |
   +---- Factory
   +---- State
   +---- Strategy
   |
   v
Repository / JDBC
   |
   v
MySQL
```

Thymeleaf render HTML phía server. Bootstrap đảm nhiệm responsive UI cơ
bản.

### Quy tắc kiến trúc

1.  Controller không chứa SQL.
2.  Controller không tự tính giá.
3.  Controller không tự thay đổi tồn kho.
4.  Controller không tự quyết định State transition.
5.  Service sở hữu business workflow.
6.  Repository sở hữu persistence.
7.  Model/domain không phụ thuộc giao diện.
8.  Không duplicate logic giữa JavaFX cũ và Web mới.
9.  JavaFX v1.0.0 được giữ làm checkpoint; Web v2.0 phát triển trên
    branch mới.

------------------------------------------------------------------------

# 5. SRS --- Yêu cầu chức năng

## SRS-FR-01 --- Đăng nhập

**Actor:** Employee\
**Priority:** Must

Hệ thống phải: - Cho nhập username/password. - Kiểm tra tài khoản từ
database. - Từ chối sai thông tin. - Chuyển tới Dashboard khi thành
công. - Có chức năng Logout. - Không lưu password DB thật trong source
code.

**Acceptance Criteria** - `admin/admin123` từ seed dùng được trong môi
trường demo. - Sai username/password không vào hệ thống. - Logout kết
thúc phiên làm việc.

------------------------------------------------------------------------

## SRS-FR-02 --- Dashboard

**Actor:** Employee\
**Priority:** Must

Dashboard phải hiển thị tối thiểu: - Tổng số khách hàng. - Tổng số
loại/thiết bị. - Số đơn đang PENDING. - Số đơn CONFIRMED/RENTED. - Số
đơn đã RETURNED. - Điều hướng nhanh tới Customer, Equipment, Rental,
Checkout.

**Should:** bảng các đơn thuê gần đây.

------------------------------------------------------------------------

## SRS-FR-03 --- Quản lý khách hàng

**Actor:** Employee\
**Priority:** Must

Chức năng: - Danh sách khách hàng. - Thêm. - Xem. - Sửa. - Tìm theo
tên/số điện thoại. - Validation dữ liệu.

Dữ liệu: - customerId - fullName - phone - email - address

**Business Rules** - ID duy nhất. - Phone duy nhất. - Full name và phone
bắt buộc.

------------------------------------------------------------------------

## SRS-FR-04 --- Category

**Actor:** Employee\
**Priority:** Must

-   Danh sách category.
-   Thêm/sửa category nếu cần cho demo.
-   Không được làm mất liên kết Equipment hiện có.

------------------------------------------------------------------------

## SRS-FR-05 --- Equipment

**Actor:** Employee\
**Priority:** Must

-   Danh sách thiết bị.
-   Thêm.
-   Xem.
-   Sửa.
-   Tìm kiếm/lọc theo category/status.
-   Hiển thị giá/ngày.
-   Hiển thị total quantity và available quantity.
-   Trạng thái AVAILABLE/khả dụng theo model hiện tại.

Dữ liệu cốt lõi: - equipmentId - name - categoryId - pricePerDay -
totalQuantity - availableQuantity - status

**Invariant** `0 <= availableQuantity <= totalQuantity`

------------------------------------------------------------------------

## SRS-FR-06 --- Camping Package Factory

**Actor:** Employee\
**Priority:** Must

Hệ thống phải minh họa Factory bằng các package: - Solo - Couple -
Family

Factory trả về tập các `equipmentId + quantity` theo cấu hình gói.

Khi chọn package tại màn hình tạo đơn: - Hệ thống điền danh sách thiết
bị. - Employee được xem nội dung trước khi tạo draft. - Việc chọn
package không được bỏ qua validation tồn kho.

Factory không tạo subclass Tent/Chair/... chỉ để minh họa pattern.

------------------------------------------------------------------------

## SRS-FR-07 --- Tạo Rental Draft

**Actor:** Employee\
**Priority:** Must

Input: - Customer. - Rental date. - Expected return date. - Danh sách
equipment + quantity hoặc Camping Package.

Khi tạo: - State = `PENDING`. - PaymentStatus = `UNPAID`. - Không trừ
tồn kho. - `actualReturnDate = null`. - Snapshot
`RentalDetail.unitPrice` từ giá hiện tại. - Totals ban đầu theo workflow
hiện hành.

**Rules** - Customer phải tồn tại. - Equipment phải hợp lệ/active. -
Quantity \> 0. - Ngày trả dự kiến không trước ngày thuê. - Duplicate
equipment phải được hợp nhất hợp lý. - Không cho tạo dữ liệu không hợp
lệ.

------------------------------------------------------------------------

## SRS-FR-08 --- Xác nhận đơn

**State:** PENDING → CONFIRMED\
**Priority:** Must

Khi Confirm: 1. Re-check tồn kho tất cả items. 2. Nếu tất cả đủ →
reserve tồn kho. 3. `available -= requested`. 4. Chuyển State sang
CONFIRMED. 5. Thao tác phải atomic.

Nếu một item thiếu: - Không item nào bị trừ. - Order vẫn PENDING. - Hiển
thị lỗi rõ ràng.

------------------------------------------------------------------------

## SRS-FR-09 --- Giao thiết bị

**State:** CONFIRMED → RENTED\
**Priority:** Must

-   Chỉ CONFIRMED được Rent.
-   Không trừ tồn kho lần hai.
-   Invalid transition phải bị từ chối.

------------------------------------------------------------------------

## SRS-FR-10 --- Hủy đơn

**Priority:** Must

### PENDING → CANCELLED

-   Không thay đổi tồn kho.

### CONFIRMED → CANCELLED

-   Hoàn trả đúng lượng đã reserve.
-   Chỉ hoàn trả một lần.

### RENTED

-   Không cho Cancel theo lifecycle hiện hành.

------------------------------------------------------------------------

## SRS-FR-11 --- Trả thiết bị

**State:** RENTED → RETURNED\
**Priority:** Must

-   Nhập actualReturnDate.
-   Validate ngày trả.
-   Hoàn tồn kho đúng một lần.
-   Set actualReturnDate.
-   State → RETURNED.
-   RETURNED là terminal state.

------------------------------------------------------------------------

## SRS-FR-12 --- State Pattern

State hợp lệ:

``` text
PENDING
   |---- confirm ----> CONFIRMED ---- rent ----> RENTED ---- return ----> RETURNED
   |                     |
   |                     +---- cancel ----> CANCELLED
   |
   +---- cancel ----> CANCELLED
```

Mỗi State chỉ cho phép transition hợp lệ. Controller không được viết
chuỗi `if/else` thay thế State Pattern.

------------------------------------------------------------------------

## SRS-FR-13 --- Pricing Strategy

**Priority:** Must

Rental days:

``` text
max(1, DAYS.between(rentalDate, expectedReturnDate))
```

Line:

``` text
unitPrice * quantity * rentalDays
```

Subtotal:

``` text
sum(lineTotal)
```

Strategies: - `NormalPricingStrategy`: discount = 0. -
`LongTermPricingStrategy`: giảm 10% khi `rentalDays >= 5`.

Tiền phải dùng `BigDecimal`.

`RentalDetail.unitPrice` là snapshot lịch sử; thay đổi giá Equipment sau
này không được làm thay đổi đơn cũ.

------------------------------------------------------------------------

## SRS-FR-14 --- Checkout

**Priority:** Must

Màn hình checkout phải: - Load RentalOrder thật từ DB. - Hiển thị
customer/order/items. - Hiển thị rental days. - Hiển thị subtotal. -
Hiển thị discount. - Hiển thị total. - Refresh/recalculate thông qua
service. - Không tự tính công thức trong Web Controller.

------------------------------------------------------------------------

## SRS-FR-15 --- Payment

**Priority:** Must

Payment status: - UNPAID - PAID

Confirm Payment: - Chỉ UNPAID → PAID. - Persist totals và payment
status. - Không đổi Rental State. - Không đổi inventory. - Repeated
payment bị từ chối. - Không tích hợp payment gateway thật.

------------------------------------------------------------------------

## SRS-FR-16 --- Rental History / Detail

**Priority:** Must

Danh sách đơn hiển thị: - Rental ID. - Customer. - Rental/return
dates. - State. - PaymentStatus. - Total.

Detail hiển thị: - Equipment. - Quantity. - Snapshot unit price. -
Totals. - Các action hợp lệ theo State hiện tại.

------------------------------------------------------------------------

## SRS-FR-17 --- Search & Filter

**Priority:** Should

-   Customer: name/phone.
-   Equipment: name/category/status.
-   Rental: ID/customer/state/payment status.

------------------------------------------------------------------------

## SRS-FR-18 --- Feedback UI

**Priority:** Must

Mọi thao tác create/update/transition/payment: - Thành công → success
message. - Validation lỗi → message cụ thể. - Business rule lỗi → không
crash. - Database lỗi → thông báo phù hợp, không lộ password/stack trace
cho người dùng.

------------------------------------------------------------------------

# 6. SRS --- Yêu cầu phi chức năng

## SRS-NFR-01 --- Maintainability

-   Layer rõ Controller/Service/Repository.
-   Không duplicate business logic.
-   Naming nhất quán.
-   Method ngắn, trách nhiệm rõ.

## SRS-NFR-02 --- Reliability

-   Lifecycle + inventory update phải atomic.
-   Rollback khi persistence thất bại.
-   Không để stock âm hoặc vượt total.

## SRS-NFR-03 --- Security

-   Không commit DB credentials.
-   Dùng environment variables/config phù hợp.
-   Không render stack trace ra UI.
-   Demo password chỉ là dữ liệu seed.

## SRS-NFR-04 --- Usability

-   Navigation nhất quán.
-   Form có label rõ.
-   State/payment dùng badge.
-   Action nguy hiểm có xác nhận.
-   Layout sử dụng tốt ở màn hình laptop.

## SRS-NFR-05 --- Performance

Với phạm vi demo/local: - Các trang CRUD thông thường phản hồi hợp lý
trên máy local. - Không query DB lặp vô nghĩa trong vòng lặp lớn. -
Không yêu cầu caching/distributed system.

## SRS-NFR-06 --- Testability

-   Business logic phải test được không cần browser.
-   Automated test không phụ thuộc MySQL live trừ test profile riêng.
-   Web Controller test có thể dùng MockMvc nếu triển khai.

## SRS-NFR-07 --- Compatibility

-   Java 21 target.
-   Maven.
-   MySQL 8.x.
-   Browser desktop hiện đại.

------------------------------------------------------------------------

# 7. Database Contract

Giữ 6 bảng hiện tại: 1. users 2. customers 3. categories 4. equipment 5.
rental_orders 6. rental_details

Không redesign schema nếu không có defect được xác nhận.

Các contract quan trọng: - `rental_orders.status`:
PENDING/CONFIRMED/RENTED/RETURNED/CANCELLED. - `payment_status`:
UNPAID/PAID. - Monetary fields: DECIMAL ↔ BigDecimal. -
`rental_details.unit_price`: historical snapshot. - Foreign keys phải
được bảo toàn. - Quantity constraints phải được bảo toàn.

------------------------------------------------------------------------

# 8. Giao diện Web cần có

## W01 Login

-   Logo/title.
-   Username/password.
-   Login button.
-   Error alert.

## W02 Dashboard

-   Navbar/sidebar.
-   Summary cards.
-   Recent rentals.
-   Quick actions.

## W03 Customers

-   Table.
-   Search.
-   Add/Edit form.
-   Detail.

## W04 Equipment

-   Table/cards.
-   Category/status filter.
-   Stock indicator.
-   Add/Edit.

## W05 Rental List

-   Rental ID.
-   Customer.
-   Dates.
-   State badge.
-   Payment badge.
-   Total.
-   Detail/action.

## W06 Create Rental

-   Customer select/search.
-   Rental dates.
-   Add equipment rows.
-   Quantity.
-   Optional package selection.
-   Draft button.

## W07 Rental Detail

-   Order information.
-   Items.
-   State.
-   Stock-relevant actions: Confirm/Rent/Cancel/Return.
-   Chỉ hiện action hợp lệ.

## W08 Checkout

-   Items + price snapshot.
-   Rental days.
-   Subtotal/discount/total.
-   Strategy result.
-   Payment status.
-   Confirm payment.

------------------------------------------------------------------------

# 9. Migration Strategy v1.0 → v2.0

## Giữ nguyên/tái sử dụng

-   Domain models.
-   Pricing Strategy.
-   State classes.
-   Factory logic.
-   RentalService business rules.
-   Checkout services.
-   Repository contracts.
-   JDBC repositories nếu phù hợp Spring wiring.
-   Database schema/seed.
-   Unit/integration tests business layer.

## Thay thế

-   JavaFX application/navigation.
-   JavaFX controllers.
-   JavaFX/FXML views.

## Bổ sung

-   Spring Boot entry point.
-   Spring configuration.
-   MVC controllers.
-   Thymeleaf templates.
-   Web static assets.
-   Session login.
-   Web-specific tests.

> Không xóa JavaFX ngay đầu migration. Chỉ cleanup sau khi Web flow đã
> PASS, để v1.0.0 luôn là fallback.

------------------------------------------------------------------------

# 10. Git Strategy

Stable:

``` text
main
tag v1.0.0
```

Tạo integration branch:

``` text
feature/web-application
```

Khuyến nghị các branch:

``` text
feature/web-foundation
feature/long-web-account-customer
feature/quan-web-equipment-package
feature/tho-web-rental
feature/thien-web-checkout
feature/web-final-integration
```

Quy tắc: - Không push trực tiếp main. - Một PR = một phạm vi rõ. -
Pull/rebase/merge integration baseline trước khi PR. - `mvn clean test`
phải PASS. - `mvn clean package` phải PASS. - `git diff --check` phải
PASS. - Không commit `.env`, password, `target/`.

------------------------------------------------------------------------

# 11. Phân công chi tiết

## D --- Lương Hữu Thiện: Foundation + Pricing/Checkout + Integration

### D0 Web Foundation

-   Tạo Spring Boot structure.
-   Thêm Spring Web + Thymeleaf dependencies.
-   Tạo base layout/navbar/error handling.
-   Wiring service/repository hiện có.
-   Thiết lập DB config bằng environment.
-   Tạo branch/integration contract.
-   Không thay business rules.

### D1 Pricing Web

-   Checkout page.
-   Hiển thị snapshot prices.
-   Normal/LongTerm Strategy.
-   Subtotal/discount/total.
-   Tests.

### D2 Payment Web

-   Confirm payment.
-   UNPAID→PAID.
-   Isolation với State/inventory.
-   Tests.

### D3 Final Integration

-   Merge/review module.
-   E2E.
-   Regression.
-   README/demo.

------------------------------------------------------------------------

## A --- Nguyễn Minh Long: Account + Customer + Dashboard

### A1 Web Login

-   Login controller/page.
-   Session.
-   Logout.
-   Error handling.

### A2 Customer

-   List/search/create/edit/detail.
-   Validation phone/full name.
-   Repository/service reuse.

### A3 Dashboard

-   Summary counts.
-   Recent rentals.
-   Navigation.

### A4 Tests

-   Login success/failure.
-   Customer validation.
-   Controller/service tests.

------------------------------------------------------------------------

## B --- Hoàng Bình Quân: Equipment + Inventory + Factory

### B1 Category/Equipment Web

-   List/search/filter.
-   Add/edit.
-   Stock display.
-   Validation.

### B2 Factory

-   Solo/Couple/Family.
-   UI package selector.
-   Package → draft item mapping.
-   Không bypass stock validation.

### B3 Inventory Visibility

-   Available/total.
-   Badge/warning khi thiếu hàng.
-   Không tự reserve từ controller.

### B4 Tests

-   Factory composition.
-   Equipment validation.
-   Package mapping.

------------------------------------------------------------------------

## C --- Nguyễn Hồng Phúc Thọ: Rental + State

### C1 Rental List/Detail

-   Rental table.
-   State/payment badge.
-   Detail.

### C2 Create Draft

-   Customer/dates/items.
-   Duplicate handling.
-   Snapshot price qua service.

### C3 Lifecycle

-   Confirm.
-   Rent.
-   Cancel.
-   Return.
-   UI chỉ hiện action hợp lệ.
-   Mọi transition gọi RentalService/State.

### C4 Tests

-   Valid transitions.
-   Invalid transitions.
-   Inventory reserve/release.
-   Multi-item atomicity.

------------------------------------------------------------------------

# 12. Kế hoạch theo buổi / checkpoint

## Buổi 1 --- Freeze & Foundation

**Thiện chủ trì** - Xác nhận v1.0.0. - Tạo web integration branch. -
Spring Boot bootstrap. - Reuse domain/service/repository. - `/` hoặc
`/login` render thành công. - DB connection test.

**Gate W0** - Build PASS. - Old business tests PASS. - Spring app
starts.

## Buổi 2 --- Module skeleton song song

-   Long: Login + Customer.
-   Quân: Equipment + Factory.
-   Thọ: Rental pages skeleton.
-   Thiện: Checkout skeleton + shared layout.

**Gate W1** - Các trang đều reachable. - Không business logic trong
controller. - Không module phá test module khác.

## Buổi 3 --- Business wiring

-   A: Customer CRUD thật.
-   B: Equipment CRUD/filter + package.
-   C: Draft + State actions.
-   D: Pricing + payment.

**Gate W2** - Core services được gọi thật. - MySQL persistence hoạt
động. - Pattern không bị thay bằng logic controller.

## Buổi 4 --- Cross-module integration

-   Package → Rental.
-   Rental → Inventory.
-   Rental → Checkout.
-   Payment isolation.
-   Dashboard counts.
-   Error handling.

**Gate W3** - Full happy path PASS.

## Buổi 5 --- Testing & UI polish

-   Regression.
-   MockMvc/service tests.
-   Validation.
-   Responsive Bootstrap.
-   Empty states/error states.
-   Không thêm feature mới.

**Gate W4** - Automated tests PASS. - Package PASS. - No credentials. -
E2E live MySQL PASS.

## Buổi 6 --- Demo & Release

-   Clean seed/demo DB.
-   README.
-   Screenshots.
-   UML.
-   Slide.
-   Demo rehearsal.
-   Tag v2.0.0 sau khi approved.

------------------------------------------------------------------------

# 13. Definition of Done theo module

Một module chỉ DONE khi: - Code compile. - Không duplicate business
logic. - Validation đúng. - DB persistence đúng. - Error path được xử
lý. - Tests PASS. - Không credentials. - PR scope sạch. - Reviewer khác
thành viên viết code đã xem. - Demo được từ UI.

------------------------------------------------------------------------

# 14. Test Plan

## Unit

-   State transitions.
-   Pricing strategies.
-   Factory.
-   Validation helpers.

## Service Integration

-   Draft không reserve.
-   Confirm reserve once.
-   Rent no second reserve.
-   Cancel confirmed restores once.
-   Return restores once.
-   Insufficient stock atomic.
-   Historical price immutable.
-   Payment isolation.
-   Long-term discount.
-   Persistence rollback.

## Web

-   Login GET/POST.
-   Customer routes.
-   Equipment routes.
-   Rental routes.
-   Checkout routes.
-   Invalid input.
-   Invalid State action.
-   Redirect/message behavior.

## Live E2E

1.  Login.
2.  Add/select customer.
3.  Check equipment stock.
4.  Create draft.
5.  Confirm.
6.  Verify stock decreased.
7.  Rent.
8.  Checkout.
9.  Verify long-term/normal price.
10. Pay.
11. Return.
12. Verify stock restored.
13. Restart app.
14. Verify order persisted.

------------------------------------------------------------------------

# 15. Demo Scenario đề xuất

Chuẩn bị: - 1 employee. - 2--3 customers. - 5 categories. - \>=5
equipment. - Một order ngắn ngày. - Một order \>=5 ngày để minh họa
Strategy.

Demo: 1. Login. 2. Dashboard. 3. Customer. 4. Equipment. 5. Chọn Camping
Package để minh họa Factory. 6. Tạo PENDING. 7. Confirm → chỉ ra
inventory giảm. 8. Rent → State chuyển, stock không giảm lần hai. 9.
Checkout → Strategy. 10. Payment → PAID nhưng State không đổi. 11.
Return → RETURNED, inventory phục hồi. 12. Mở lịch sử/detail.

Trong demo phải nói rõ **pattern nào đang hoạt động**, không chỉ bấm
giao diện.

------------------------------------------------------------------------

# 16. Các lỗi thiết kế phải tránh

-   Viết SQL trong controller.
-   Copy công thức pricing sang Thymeleaf/controller.
-   `if(status == ...)` khắp controller thay cho State.
-   Factory chỉ tạo object vô nghĩa để "có pattern".
-   Trừ stock khi draft.
-   Trừ stock lần hai khi rent.
-   Payment làm đổi State.
-   Dùng giá Equipment hiện tại cho order lịch sử.
-   Hard-code password DB.
-   Để mỗi thành viên tạo một DB schema khác.
-   Merge trực tiếp main.
-   Rewrite toàn bộ business layer chỉ vì chuyển Web.

------------------------------------------------------------------------

# 17. Rủi ro và phương án

  -----------------------------------------------------------------------
  Rủi ro                              Xử lý
  ----------------------------------- -----------------------------------
  Migration phá business logic        Giữ v1.0.0 + regression tests

  Conflict do 4 người sửa cùng file   Chia controller/template/module,
                                      integration owner review

  Spring wiring phức tạp              Foundation hoàn tất trước khi chia
                                      module

  DB config khác máy                  Environment variables + README

  UI tốn thời gian                    Thymeleaf + Bootstrap, không SPA

  Factory khó gắn UI                  Package selector map thành
                                      RentalRequest items

  Demo lỗi dữ liệu                    Chuẩn bị seed/demo scenario và
                                      reset procedure

  Scope phình                         Must trước, Should sau, ngoài phạm
                                      vi không làm
  -----------------------------------------------------------------------

------------------------------------------------------------------------

# 18. Tiêu chí nghiệm thu Web v2.0

Web v2.0 chỉ được coi là hoàn tất khi: - Spring Boot start thành công. -
MySQL kết nối thành công. - Login/logout PASS. - Customer PASS. -
Equipment/inventory PASS. - Factory package PASS. - Rental draft PASS. -
State lifecycle PASS. - Pricing Strategy PASS. - Checkout/payment
PASS. - Full E2E PASS. - Inventory invariant PASS. - Historical unit
price PASS. - Persistence qua restart PASS. - Automated tests PASS. -
Maven package PASS. - Không credentials. - README đủ setup. - 4 Design
Patterns có code + demo + giải thích. - PR final được review. - Sau
approval mới tag `v2.0.0`.

------------------------------------------------------------------------

# 19. Thứ tự bắt đầu cho cả nhóm

**Không để 4 người code Web cùng lúc ngay từ phút đầu.**

1.  Thiện tạo Web Foundation và contract chung.
2.  Cả nhóm pull baseline Web đã PASS.
3.  Mỗi thành viên tạo branch module riêng.
4.  A/B/C/D làm đúng phạm vi.
5.  PR vào Web integration branch.
6.  Integration/E2E.
7.  Chỉ sau khi tất cả PASS mới merge main/tag v2.0.0.

------------------------------------------------------------------------

# 20. Checklist gửi thành viên

-   [ ] Đọc SRS module mình.
-   [ ] Pull baseline mới nhất.
-   [ ] Làm trên branch riêng.
-   [ ] Không đổi business rule nếu chưa thống nhất.
-   [ ] Không commit credentials.
-   [ ] Viết/giữ test.
-   [ ] `mvn clean test` PASS.
-   [ ] `mvn clean package` PASS.
-   [ ] `git diff --check` PASS.
-   [ ] Tự demo module trước PR.
-   [ ] Ghi rõ files changed + tests + known issues trong PR.

------------------------------------------------------------------------

## Kết luận

Web v2.0 là **migration lớp trình bày**, không phải viết lại project từ
đầu. Mục tiêu quan trọng nhất là chứng minh kiến trúc và bốn Design
Pattern vẫn giữ nguyên khi thay JavaFX bằng Web. Điều này vừa tăng chất
lượng sản phẩm trình diễn, vừa thể hiện đúng giá trị của thiết kế phần
mềm: business logic có thể tái sử dụng và ít phụ thuộc giao diện.
