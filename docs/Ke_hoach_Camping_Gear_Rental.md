# KẾ HOẠCH ĐỀ TÀI -- CAMPING GEAR RENTAL

> **Tên đề tài:** Xây dựng hệ thống quản lý cho thuê thiết bị cắm trại
> áp dụng các mẫu thiết kế bằng Java\
> **Quy mô:** Nhóm 4 thành viên\
> **Hướng triển khai:** Java Desktop Application\
> **Công nghệ đề xuất:** Java 17+, JavaFX, Maven, MySQL, JDBC,
> Git/GitHub\
> **Mục tiêu:** Xây dựng một prototype nhỏ, chạy được luồng thuê--trả
> hoàn chỉnh và thể hiện rõ việc áp dụng Design Pattern. Không đặt mục
> tiêu xây dựng một hệ thống thương mại hoàn chỉnh.

------------------------------------------------------------------------

## 1. Mục tiêu đề tài

Hệ thống hỗ trợ nhân viên cửa hàng:

1.  Đăng nhập.
2.  Quản lý khách hàng.
3.  Quản lý thiết bị cắm trại.
4.  Kiểm tra số lượng thiết bị khả dụng.
5.  Tạo và quản lý đơn thuê.
6.  Xác nhận và bàn giao thiết bị.
7.  Nhận trả thiết bị và cập nhật lại kho.
8.  Tính tiền thuê theo chính sách giá.
9.  Xem thông tin thanh toán/hóa đơn đơn giản.
10. Minh họa rõ các Design Pattern trong mã nguồn.

### Design Pattern bắt buộc

-   **Singleton** -- quản lý kết nối/cấu hình dùng chung.
-   **Factory** -- tạo các gói cắm trại.
-   **State** -- quản lý vòng đời đơn thuê.
-   **Strategy** -- thay đổi chính sách tính giá.

### Design Pattern mở rộng

-   **Observer** -- thông báo khi trạng thái đơn thay đổi.
-   **Decorator** -- bổ sung dịch vụ vào gói thuê.

> Nhóm chỉ triển khai Observer và Decorator nếu các chức năng bắt buộc
> đã hoàn thành ổn định.

------------------------------------------------------------------------

# 2. Phạm vi hệ thống

## 2.1. Actor

Phiên bản đầu chỉ có một actor chính:

### Nhân viên

Nhân viên có quyền:

-   Đăng nhập.
-   Quản lý khách hàng.
-   Quản lý thiết bị.
-   Tạo đơn thuê.
-   Xác nhận đơn.
-   Giao thiết bị.
-   Nhận trả thiết bị.
-   Xem tổng tiền.

Không xây dựng website/app riêng cho khách hàng.

------------------------------------------------------------------------

# 3. Yêu cầu chức năng

## FR01 -- Đăng nhập

Nhân viên nhập:

-   Username.
-   Password.

Hệ thống:

-   Kiểm tra thông tin đăng nhập.
-   Đúng → chuyển vào màn hình chính.
-   Sai → hiển thị thông báo.

### Không yêu cầu

-   Đăng ký.
-   Quên mật khẩu.
-   OTP.
-   Phân quyền nhiều cấp.

------------------------------------------------------------------------

## FR02 -- Quản lý khách hàng

Thông tin:

``` text
Customer
- customerId
- fullName
- phone
- email
- address
```

Chức năng:

-   Xem danh sách.
-   Thêm.
-   Sửa.
-   Xóa.
-   Tìm theo tên hoặc số điện thoại.

Ràng buộc:

-   Tên không được rỗng.
-   Số điện thoại không được trùng.

------------------------------------------------------------------------

## FR03 -- Quản lý danh mục thiết bị

Danh mục mẫu:

-   Lều.
-   Túi ngủ.
-   Ghế.
-   Bàn.
-   Đèn.
-   Bếp.
-   Khác.

Có thể tạo dữ liệu danh mục ban đầu và không cần làm màn hình CRUD riêng
nếu thiếu thời gian.

------------------------------------------------------------------------

## FR04 -- Quản lý thiết bị

``` text
Equipment
- equipmentId
- name
- categoryId
- pricePerDay
- totalQuantity
- availableQuantity
- status
```

Chức năng:

-   Xem danh sách.
-   Thêm.
-   Sửa.
-   Xóa.
-   Tìm kiếm.
-   Lọc theo danh mục.
-   Xem số lượng còn có thể thuê.

Ví dụ:

  Mã     Tên                 Loại     Giá/ngày   Tổng SL   Khả dụng
  ------ ------------------- ------ ---------- --------- ----------
  EQ01   Lều NatureHike 2P   Lều       100.000         5          5
  EQ02   Ghế camping         Ghế        30.000        10         10
  EQ03   Đèn camping         Đèn        20.000         8          8
  EQ04   Bếp mini            Bếp        50.000         4          4

------------------------------------------------------------------------

## FR05 -- Kiểm tra thiết bị khả dụng

Không được xác nhận thuê số lượng lớn hơn `availableQuantity`.

Ví dụ:

``` text
availableQuantity = 3
requestedQuantity = 5

=> Không đủ thiết bị.
```

Việc kiểm tra phải thực hiện lại khi **Confirm đơn**, không chỉ khi
người dùng thêm sản phẩm vào đơn.

------------------------------------------------------------------------

## FR06 -- Tạo đơn thuê

Nhân viên:

1.  Chọn khách hàng.
2.  Chọn ngày thuê.
3.  Chọn ngày trả dự kiến.
4.  Thêm một hoặc nhiều thiết bị.
5.  Nhập số lượng.
6.  Tạo đơn.

Đơn mới có trạng thái:

``` text
PENDING
```

`PENDING` chưa giữ hàng.

------------------------------------------------------------------------

## FR07 -- Chi tiết đơn thuê

``` text
RentalOrder
- id
- customerId
- rentalDate
- expectedReturnDate
- actualReturnDate
- status
- subtotal
- discount
- total
- paymentStatus
- createdAt
```

``` text
RentalDetail
- id
- rentalOrderId
- equipmentId
- quantity
- unitPrice
```

### Quy tắc quan trọng

`RentalDetail.unitPrice` phải lưu giá thiết bị **tại thời điểm lập
đơn**.

Nếu giá thiết bị thay đổi sau này, hóa đơn cũ không được thay đổi.

------------------------------------------------------------------------

## FR08 -- Quản lý trạng thái đơn

Luồng chuẩn:

``` text
PENDING
   |
   +----> CONFIRMED ----> RENTED ----> RETURNED
   |          |
   |          +----> CANCELLED
   |
   +----> CANCELLED
```

### PENDING

-   Đơn mới tạo.
-   Chưa giữ thiết bị.
-   Có thể Confirm.
-   Có thể Cancel.

### CONFIRMED

-   Đơn đã được cửa hàng xác nhận.
-   Thiết bị đã được giữ.
-   Có thể chuyển sang Rented.
-   Có thể Cancel.

### RENTED

-   Khách đã nhận thiết bị.
-   Có thể chuyển sang Returned.
-   Không được Cancel.

### RETURNED

-   Khách đã trả thiết bị.
-   Trạng thái kết thúc.

### CANCELLED

-   Đơn bị hủy.
-   Trạng thái kết thúc.

### Chuyển trạng thái không hợp lệ

``` text
RETURNED -> RENTED      X
CANCELLED -> CONFIRMED  X
RENTED -> CANCELLED     X
```

------------------------------------------------------------------------

# 4. Logic tồn kho

Đây là nghiệp vụ bắt buộc phải triển khai thống nhất.

## Khi tạo PENDING

``` text
availableQuantity không thay đổi
```

## PENDING → CONFIRMED

Kiểm tra lại:

``` text
requestedQuantity <= availableQuantity
```

Nếu đủ:

``` text
availableQuantity -= requestedQuantity
```

Nếu không đủ:

-   Không Confirm.
-   Đơn vẫn PENDING.
-   Hiển thị thông báo.

## CONFIRMED → RENTED

``` text
availableQuantity không thay đổi
```

Vì thiết bị đã được giữ từ lúc Confirm.

## CONFIRMED → CANCELLED

``` text
availableQuantity += reservedQuantity
```

## RENTED → RETURNED

``` text
availableQuantity += returnedQuantity
```

### Quy tắc

``` text
0 <= availableQuantity <= totalQuantity
```

------------------------------------------------------------------------

# 5. Logic số ngày thuê

Quy ước:

``` text
rentalDays = max(
    1,
    DAYS.between(rentalDate, expectedReturnDate)
)
```

Ví dụ:

  Thuê    Trả       Số ngày tính
  ------- ------- --------------
  20/10   20/10                1
  20/10   21/10                1
  20/10   23/10                3

Ngày trả dự kiến không được trước ngày thuê.

------------------------------------------------------------------------

# 6. Logic tính tiền

Mỗi dòng:

``` text
lineTotal =
unitPrice
x quantity
x rentalDays
```

Tạm tính:

``` text
subtotal = tổng tất cả lineTotal
```

Giảm giá:

``` text
discount = PricingStrategy tính
```

Tổng:

``` text
total = subtotal - discount
```

### Ví dụ

``` text
Lều:
100.000 x 1 x 3 = 300.000

Ghế:
30.000 x 2 x 3 = 180.000

Đèn:
20.000 x 1 x 3 = 60.000

Subtotal = 540.000
```

Trong Java nên dùng:

``` java
BigDecimal
```

Không dùng `double` để lưu tiền.

------------------------------------------------------------------------

# 7. Chính sách giá -- Strategy Pattern

Interface dự kiến:

``` java
public interface PricingStrategy {
    BigDecimal calculateDiscount(
        BigDecimal subtotal,
        long rentalDays
    );
}
```

Các Strategy tối thiểu:

``` text
NormalPricingStrategy
LongTermPricingStrategy
```

### Normal

Không giảm giá.

### Long Term

Ví dụ chính sách:

``` text
rentalDays >= 5
=> giảm 10%
```

Service chịu trách nhiệm chọn Strategy phù hợp.

Có thể mở rộng sau:

``` text
MemberPricingStrategy
WeekendPricingStrategy
HolidayPricingStrategy
```

------------------------------------------------------------------------

# 8. State Pattern

Interface/lớp cơ sở:

``` text
RentalState
```

Các State:

``` text
PendingState
ConfirmedState
RentedState
ReturnedState
CancelledState
```

`RentalOrder` giữ:

``` text
currentState
```

Trách nhiệm:

  State       Hành động hợp lệ
  ----------- ---------------------
  Pending     confirm(), cancel()
  Confirmed   rent(), cancel()
  Rented      returnEquipment()
  Returned    Không chuyển tiếp
  Cancelled   Không chuyển tiếp

Không viết toàn bộ logic bằng chuỗi `if/else` dựa trên status vì sẽ làm
mất ý nghĩa của State Pattern.

------------------------------------------------------------------------

# 9. Singleton Pattern

Áp dụng cho:

``` text
DatabaseConnection
```

Ví dụ mục tiêu:

``` java
DatabaseConnection.getInstance()
```

Toàn ứng dụng sử dụng cùng cơ chế quản lý kết nối/cấu hình database.

------------------------------------------------------------------------

# 10. Factory Pattern

Không tạo subclass Tent/Chair/Lamp chỉ để minh họa Factory nếu chúng
không có hành vi khác nhau.

Factory được áp dụng tự nhiên hơn cho **Camping Package**.

``` text
CampingPackageFactory
|
+-- SoloPackage
+-- CouplePackage
+-- FamilyPackage
```

Ví dụ:

### Solo Package

-   1 lều.
-   1 túi ngủ.
-   1 đèn.

### Couple Package

-   1 lều 2 người.
-   2 túi ngủ.
-   2 ghế.
-   1 đèn.

### Family Package

-   Lều lớn.
-   Túi ngủ.
-   Bàn.
-   Ghế.
-   Đèn.

Factory tạo cấu hình package tương ứng.

> Nếu thời gian hạn chế, Factory chỉ cần được minh họa rõ bằng
> logic/package; không cần xây hệ thống combo quá phức tạp.

------------------------------------------------------------------------

# 11. Observer Pattern -- mở rộng

Khi trạng thái RentalOrder thay đổi:

``` text
RentalOrder
    |
    +--> NotificationObserver
```

Ví dụ:

``` text
Đơn R001 đã được xác nhận.
Đơn R001 đã được giao cho khách.
Đơn R001 đã hoàn tất trả thiết bị.
```

Chỉ cần notification trong ứng dụng.

Không cần:

-   Email.
-   SMS.
-   Push notification thật.

------------------------------------------------------------------------

# 12. Decorator Pattern -- mở rộng

Nếu triển khai, Decorator dùng cho **dịch vụ bổ sung**, không dùng đơn
thuần để thêm Equipment vào RentalDetail.

Ví dụ:

``` text
BasicCampingPackage
    |
    + DeliveryDecorator
    |
    + CleaningDecorator
    |
    + InsuranceDecorator
```

Ví dụ:

``` text
Basic Package       400.000
Delivery             50.000
Cleaning             30.000
---------------------------
Total                480.000
```

------------------------------------------------------------------------

# 13. Trả thiết bị

Chỉ đơn `RENTED` mới được trả.

Khi trả:

1.  Chuyển `RENTED → RETURNED`.
2.  Ghi `actualReturnDate`.
3.  Hoàn lại `availableQuantity`.
4.  Đơn trở thành trạng thái kết thúc.

Phiên bản đầu không cần xử lý:

-   Thiết bị hỏng.
-   Thiết bị mất.
-   Phí trả trễ.

Có thể bổ sung sau nếu còn thời gian.

------------------------------------------------------------------------

# 14. Thanh toán

Không tích hợp cổng thanh toán thật.

Chỉ cần:

``` text
paymentStatus
- UNPAID
- PAID
```

Màn hình Checkout hiển thị:

``` text
CAMPING GEAR RENTAL

Khách hàng: Nguyễn Văn A

Lều      1 x 3 ngày     300.000
Ghế      2 x 3 ngày     180.000
Đèn      1 x 3 ngày      60.000

Subtotal                540.000
Discount                      0
-------------------------------
TOTAL                   540.000
```

Có nút:

``` text
Xác nhận thanh toán
```

Không bắt buộc xuất PDF.

------------------------------------------------------------------------

# 15. Business Rules

  ID     Quy tắc
  ------ ---------------------------------------------------------
  BR01   Nhân viên phải đăng nhập trước khi sử dụng hệ thống
  BR02   Tên khách hàng không được rỗng
  BR03   Số điện thoại khách hàng không được trùng
  BR04   Giá thuê thiết bị phải \> 0
  BR05   totalQuantity và availableQuantity không được âm
  BR06   availableQuantity \<= totalQuantity
  BR07   Ngày trả dự kiến không được trước ngày thuê
  BR08   Một đơn phải có ít nhất một thiết bị
  BR09   Số lượng thuê của mỗi thiết bị phải \> 0
  BR10   Khi Confirm phải kiểm tra lại tồn kho
  BR11   Không Confirm nếu số lượng yêu cầu \> availableQuantity
  BR12   Confirm → giảm availableQuantity
  BR13   Cancel đơn Confirmed → hoàn availableQuantity
  BR14   Confirmed → Rented không thay đổi tồn kho
  BR15   Returned → hoàn availableQuantity
  BR16   Số ngày thuê tối thiểu = 1
  BR17   RentalDetail lưu unitPrice tại thời điểm lập đơn
  BR18   subtotal = Σ(unitPrice × quantity × rentalDays)
  BR19   Strategy quyết định giảm giá
  BR20   total = subtotal - discount
  BR21   Returned và Cancelled là trạng thái kết thúc
  BR22   Chỉ Confirmed mới chuyển sang Rented
  BR23   Chỉ Rented mới chuyển sang Returned
  BR24   Chỉ Pending/Confirmed được Cancel

------------------------------------------------------------------------

# 16. Database tối thiểu

Các bảng:

``` text
users
customers
categories
equipment
rental_orders
rental_details
```

## Quan hệ

``` text
Category 1 ------ N Equipment

Customer 1 ------ N RentalOrder

RentalOrder 1 --- N RentalDetail

Equipment 1 ----- N RentalDetail
```

## users

``` text
id
username
password
```

> Với prototype môn học có thể dùng cơ chế đăng nhập đơn giản; nếu triển
> khai thực tế phải lưu mật khẩu dưới dạng hash an toàn.

## customers

``` text
id
full_name
phone
email
address
```

## categories

``` text
id
name
```

## equipment

``` text
id
name
category_id
price_per_day
total_quantity
available_quantity
status
```

## rental_orders

``` text
id
customer_id
rental_date
expected_return_date
actual_return_date
status
subtotal
discount
total
payment_status
created_at
```

## rental_details

``` text
id
rental_order_id
equipment_id
quantity
unit_price
```

------------------------------------------------------------------------

# 17. Kiến trúc source code

Đề xuất:

``` text
src/main/java/
|
+-- model/
+-- state/
+-- strategy/
+-- factory/
+-- singleton/
+-- observer/       # optional
+-- decorator/      # optional
+-- repository/
+-- service/
+-- controller/
+-- util/
|
src/main/resources/
|
+-- view/
+-- css/
```

Luồng:

``` text
JavaFX View
    |
Controller
    |
Service
    |
Repository / DAO
    |
MySQL
```

------------------------------------------------------------------------

# 18. Các màn hình cần làm

## Bắt buộc

1.  Login.
2.  Main Menu.
3.  Customer Management.
4.  Equipment Management.
5.  Rental Management.
6.  Rental Detail / Checkout.

## Không cần đầu tư nhiều

Dashboard chỉ cần:

-   Tên hệ thống.
-   Menu điều hướng.
-   Có thể hiển thị vài số liệu đơn giản nếu còn thời gian.

------------------------------------------------------------------------

# 19. Use Case chính

``` text
Employee
|
+-- Login
+-- Manage Customer
|   +-- Add
|   +-- Edit
|   +-- Delete
|   +-- Search
|
+-- Manage Equipment
|   +-- Add
|   +-- Edit
|   +-- Delete
|   +-- Search
|
+-- Manage Rental
|   +-- Create Rental
|   +-- Confirm Rental
|   +-- Hand Over Equipment
|   +-- Return Equipment
|   +-- Cancel Rental
|
+-- Calculate Price
+-- View Checkout
+-- Confirm Payment
```

------------------------------------------------------------------------

# 20. Phân công 4 phần

Phân công chính thức 4 thành viên theo **A/B/C/D**.

  Phần   Module chính                       Pattern chính
  ------ ---------------------------------- ------------------------
  A      Account + Customer + nền tảng DB   Singleton
  B      Equipment + Inventory              Factory hỗ trợ Package
  C      Rental + Rent/Return               State
  D      Pricing + Checkout + Package       Strategy

Observer và Decorator được làm chung hoặc giao cho C/D nếu còn thời
gian.

------------------------------------------------------------------------

# 21. Nhiệm vụ chi tiết theo thành viên

## A -- Account, Customer & Database

### Phụ trách

-   DatabaseConnection.
-   Login.
-   Customer.
-   Repository/DAO khách hàng.
-   Customer UI.
-   Singleton Pattern.
-   Hỗ trợ schema/database chung.

### Class dự kiến

``` text
User
Customer
DatabaseConnection
UserRepository
CustomerRepository
AuthService
CustomerService
LoginController
CustomerController
```

### Kết quả

-   Login chạy.
-   Customer CRUD chạy.
-   Search Customer chạy.
-   Singleton giải thích/demo được.

------------------------------------------------------------------------

## B -- Equipment & Inventory

### Phụ trách

-   Category.
-   Equipment.
-   Equipment CRUD.
-   Search/filter.
-   totalQuantity.
-   availableQuantity.
-   Hỗ trợ logic giữ/trả kho.
-   Factory cho Camping Package phối hợp với D.

### Class dự kiến

``` text
Category
Equipment
EquipmentRepository
EquipmentService
EquipmentController
CampingPackage
CampingPackageFactory
```

### Kết quả

-   CRUD Equipment.
-   Kiểm tra số lượng.
-   Không cho số lượng âm.
-   Hiển thị số lượng khả dụng.
-   Factory chạy được.

------------------------------------------------------------------------

## C -- Rental & State

### Phụ trách

-   RentalOrder.
-   RentalDetail.
-   Create Rental.
-   Confirm.
-   Rent.
-   Return.
-   Cancel.
-   State Pattern.
-   Tích hợp tồn kho với B.

### Class dự kiến

``` text
RentalOrder
RentalDetail

RentalState
PendingState
ConfirmedState
RentedState
ReturnedState
CancelledState

RentalRepository
RentalService
RentalController
```

### Kết quả

-   Tạo đơn.
-   Chuyển State đúng.
-   Chặn State sai.
-   Confirm giữ hàng.
-   Cancel hoàn hàng.
-   Return hoàn hàng.

------------------------------------------------------------------------

## D -- Pricing, Checkout & Package

### Phụ trách

-   Tính số ngày.
-   Tính subtotal.
-   Discount.
-   Total.
-   Strategy Pattern.
-   Checkout.
-   Payment status.
-   Phối hợp Factory Package với B.

### Class dự kiến

``` text
PricingStrategy
NormalPricingStrategy
LongTermPricingStrategy
PricingService
CheckoutController
```

### Kết quả

-   Giá tính chính xác.
-   BigDecimal.
-   Normal/LongTerm chạy.
-   Checkout hiển thị đúng.
-   Có thể xác nhận PAID.

------------------------------------------------------------------------

# 22. Kế hoạch 8 buổi nhẹ

Mỗi buổi dự kiến khoảng **1,5--2 giờ**.

## Buổi 1 -- Chốt thiết kế

### Cả nhóm

-   Chốt scope.
-   Tạo GitHub repository.
-   Tạo Maven project.
-   Chốt naming convention.
-   Vẽ ERD bản đầu.
-   Vẽ Use Case.
-   Vẽ Class Diagram bản đầu.
-   Tạo branch từng thành viên.

### Nguyễn Minh Long (A)

-   Chốt User/Customer.
-   Nghiên cứu Singleton.

### Hoàng Bình Quân (B)

-   Chốt Category/Equipment.
-   Nghiên cứu Factory.

### Nguyễn Hồng Phúc Thọ (C)

-   Chốt RentalOrder/RentalDetail.
-   Chốt State transition.

### Lương Hữu Thiện (D)

-   Chốt Pricing.
-   Nghiên cứu Strategy.

### Deliverable

``` text
README
ERD
Use Case
Class Diagram v1
Project skeleton
Branches
```

------------------------------------------------------------------------

## Buổi 2 -- Database & Model

### Nguyễn Minh Long (A)

-   Tạo users.
-   Tạo customers.
-   DatabaseConnection.
-   User/Customer model.

### Hoàng Bình Quân (B)

-   Tạo categories.
-   Tạo equipment.
-   Equipment model.

### Nguyễn Hồng Phúc Thọ (C)

-   Tạo rental_orders.
-   Tạo rental_details.
-   Rental model.

### Lương Hữu Thiện (D)

-   Xây Pricing model/interface cơ bản.
-   Chuẩn bị dữ liệu test.

### Deliverable

-   Database chạy.
-   Java kết nối DB.
-   Model compile.
-   Có dữ liệu mẫu.

------------------------------------------------------------------------

## Buổi 3 -- Chức năng CRUD/logic cơ bản

### Nguyễn Minh Long (A)

-   Login.
-   Customer CRUD.
-   Search Customer.

### Hoàng Bình Quân (B)

-   Equipment CRUD.
-   Search/filter Equipment.
-   Validation quantity/price.

### Nguyễn Hồng Phúc Thọ (C)

-   Create Rental.
-   Add RentalDetail.
-   Validate ngày và quantity.

### Lương Hữu Thiện (D)

-   rentalDays.
-   subtotal.
-   total cơ bản.

### Deliverable

Các module chạy logic độc lập.

------------------------------------------------------------------------

## Buổi 4 -- Design Pattern chính

### Nguyễn Minh Long (A)

Hoàn thiện:

``` text
Singleton
```

### Hoàng Bình Quân (B)

Hoàn thiện:

``` text
Factory
```

### Nguyễn Hồng Phúc Thọ (C)

Hoàn thiện:

``` text
State
```

### Lương Hữu Thiện (D)

Hoàn thiện:

``` text
Strategy
```

### Deliverable

Mỗi người phải demo được Pattern của mình bằng code.

------------------------------------------------------------------------

## Buổi 5 -- Hoàn thiện nghiệp vụ

### Nguyễn Minh Long (A)

-   Validation Customer.
-   Kiểm tra Login.
-   Cleanup code.

### Hoàng Bình Quân (B)

-   Logic availableQuantity.
-   Hỗ trợ C khi reserve/return.

### Nguyễn Hồng Phúc Thọ (C)

Hoàn thiện:

``` text
PENDING -> CONFIRMED
CONFIRMED -> RENTED
RENTED -> RETURNED
PENDING/CONFIRMED -> CANCELLED
```

### Lương Hữu Thiện (D)

-   NormalPricing.
-   LongTermPricing.
-   BigDecimal.
-   Checkout data.

### Cả nhóm

Test logic tồn kho + giá.

### Deliverable

Luồng backend/service hoàn chỉnh.

------------------------------------------------------------------------

## Buổi 6 -- JavaFX

### Nguyễn Minh Long (A)

-   Login UI.
-   Customer UI.

### Hoàng Bình Quân (B)

-   Equipment UI.

### Nguyễn Hồng Phúc Thọ (C)

-   Rental UI.
-   Rental Detail UI.

### Lương Hữu Thiện (D)

-   Checkout UI.

### Quy tắc

Không dành quá nhiều thời gian làm đẹp.

Ưu tiên:

``` text
Dễ dùng
Dễ demo
Không lỗi
Thống nhất
```

------------------------------------------------------------------------

## Buổi 7 -- Integration & Testing

Cả nhóm merge.

Test luồng:

``` text
Login
  ->
Customer
  ->
Equipment
  ->
Create Rental
  ->
Calculate Price
  ->
Confirm
  ->
Rent
  ->
Return
  ->
Checkout
```

### Test bắt buộc

-   Login sai.
-   Customer phone trùng.
-   Giá \<= 0.
-   Quantity âm.
-   Thuê quá số lượng.
-   Ngày trả trước ngày thuê.
-   Confirm khi hết hàng.
-   Cancel đơn Confirmed.
-   Return đơn Rented.
-   State transition sai.
-   LongTerm discount.
-   Giá cũ không thay đổi khi Equipment đổi giá.

### Deliverable

Một bản chạy end-to-end ổn định.

------------------------------------------------------------------------

## Buổi 8 -- Hoàn thiện nộp bài

### Cả nhóm

-   Fix bug cuối.
-   Dọn code.
-   Xóa code/debug không cần thiết.
-   Class Diagram cuối.
-   ERD cuối.
-   Screenshot.
-   README.
-   Báo cáo.
-   Slide nếu được yêu cầu.
-   Chuẩn bị demo.

### Nguyễn Minh Long (A) trình bày

-   Kiến trúc.
-   Database.
-   Customer.
-   Singleton.

### Hoàng Bình Quân (B) trình bày

-   Equipment.
-   Inventory.
-   Factory.

### Nguyễn Hồng Phúc Thọ (C) trình bày

-   Rental.
-   State.

### Lương Hữu Thiện (D) trình bày

-   Pricing.
-   Strategy.
-   Checkout.

------------------------------------------------------------------------

# 23. Git workflow

Khuyến nghị:

``` text
main
|
+-- feature/member-a-account-customer
+-- feature/member-b-equipment
+-- feature/member-c-rental
+-- feature/member-d-pricing
```

Quy trình:

``` text
git checkout main
git pull origin main

git checkout <branch-cua-minh>

# code

git add .
git commit -m "feat: ..."

git push origin <branch-cua-minh>
```

Sau đó Pull Request vào `main`.

### Quy tắc nhóm

-   Không code trực tiếp lên `main`.
-   Pull main mới trước khi bắt đầu buổi làm.
-   Commit nhỏ và rõ nội dung.
-   Không commit file IDE/cache/build không cần thiết.
-   Test trước khi merge.
-   Một người review nhanh trước khi merge.

------------------------------------------------------------------------

# 24. Quy ước code

## Package

Tên package viết thường.

``` text
model
service
repository
controller
state
strategy
factory
```

## Class

PascalCase:

``` text
RentalOrder
PricingStrategy
EquipmentService
```

## Method/variable

camelCase:

``` text
calculateTotal()
availableQuantity
customerId
```

## Constant

``` text
UPPER_SNAKE_CASE
```

## Không làm

-   Business logic trực tiếp trong JavaFX Controller quá nhiều.
-   SQL nằm rải rác trong UI.
-   Một class xử lý toàn bộ hệ thống.
-   Dùng `double` cho tiền.
-   So sánh State bằng hàng loạt `if/else` nếu đã dùng State Pattern.

------------------------------------------------------------------------

# 25. Dữ liệu test mẫu

## Account

``` text
username: admin
password: admin123
```

## Customer

``` text
CUS001
Nguyễn Văn A
0900000001
a@example.com
Đà Lạt
```

## Equipment

``` text
EQ001 | Lều 2 người | Tent | 100000 | 5
EQ002 | Túi ngủ | Sleeping Bag | 40000 | 10
EQ003 | Ghế camping | Chair | 30000 | 10
EQ004 | Đèn camping | Lamp | 20000 | 8
EQ005 | Bếp mini | Stove | 50000 | 4
```

------------------------------------------------------------------------

# 26. Test case quan trọng

  ID     Trường hợp                            Kết quả mong đợi
  ------ ------------------------------------- ------------------------------------
  TC01   Login đúng                            Vào Main Menu
  TC02   Login sai                             Báo lỗi
  TC03   Thêm Customer hợp lệ                  Thành công
  TC04   Phone Customer trùng                  Từ chối
  TC05   Equipment price \<= 0                 Từ chối
  TC06   Equipment quantity \< 0               Từ chối
  TC07   Tạo Rental không có item              Từ chối
  TC08   Ngày trả trước ngày thuê              Từ chối
  TC09   Thuê quá available                    Từ chối Confirm
  TC10   Confirm đủ hàng                       Giảm available
  TC11   Cancel Confirmed                      Hoàn available
  TC12   Confirmed → Rented                    Thành công, kho không giảm lần hai
  TC13   Rented → Returned                     Thành công, hoàn kho
  TC14   Returned → Rented                     Từ chối
  TC15   Thuê \< 5 ngày                        Normal Pricing
  TC16   Thuê \>= 5 ngày                       Long Term Pricing
  TC17   Đổi Equipment price sau khi tạo đơn   Giá đơn cũ không đổi

------------------------------------------------------------------------

# 27. Definition of Done

Project được xem là hoàn thành khi:

-   [ ] Login hoạt động.
-   [ ] Customer CRUD hoạt động.
-   [ ] Equipment CRUD hoạt động.
-   [ ] Search cơ bản hoạt động.
-   [ ] Tạo Rental được.
-   [ ] Một Rental chứa nhiều RentalDetail.
-   [ ] Kiểm tra tồn kho đúng.
-   [ ] Confirm giữ thiết bị.
-   [ ] Cancel hoàn thiết bị.
-   [ ] Rent không trừ kho lần hai.
-   [ ] Return hoàn thiết bị.
-   [ ] State chặn chuyển trạng thái sai.
-   [ ] Giá tính bằng BigDecimal.
-   [ ] Strategy Normal hoạt động.
-   [ ] Strategy LongTerm hoạt động.
-   [ ] Factory được minh họa rõ.
-   [ ] Singleton hoạt động.
-   [ ] JavaFX có các màn hình chính.
-   [ ] Database lưu dữ liệu đúng.
-   [ ] Luồng demo end-to-end chạy được.
-   [ ] README có hướng dẫn chạy.
-   [ ] Class Diagram khớp code cuối.
-   [ ] Mỗi thành viên giải thích được phần mình phụ trách.

------------------------------------------------------------------------

# 28. Chức năng mở rộng -- chỉ làm khi dư thời gian

Ưu tiên theo thứ tự:

1.  Observer Notification.
2.  Decorator Extra Service.
3.  Dashboard thống kê đơn giản.
4.  Combo đẹp hơn.
5.  Tìm kiếm/lọc nâng cao.
6.  Phí trả trễ.
7.  Xử lý thiết bị hỏng/mất.

Không được làm các phần mở rộng trước khi chức năng bắt buộc ổn định.

------------------------------------------------------------------------

# 29. Những phần không làm

Để tránh project phình quá lớn:

-   Không website khách hàng.
-   Không mobile app.
-   Không AI.
-   Không GPS/map.
-   Không thanh toán MoMo/VNPay thật.
-   Không email/SMS thật.
-   Không quản lý giao hàng.
-   Không quản lý nhiều chi nhánh.
-   Không recommendation.
-   Không authentication phức tạp.
-   Không microservices.

------------------------------------------------------------------------

# 30. Rủi ro và cách xử lý

  -----------------------------------------------------------------------
  Rủi ro                              Cách xử lý
  ----------------------------------- -----------------------------------
  Thành viên làm không đồng bộ        Chốt interface/model trước khi code

  Merge conflict                      Mỗi người làm branch/module riêng

  JavaFX tốn thời gian                UI tối giản, ưu tiên chức năng

  Pattern bị dùng gượng ép            Chỉ giữ 4 pattern chính có lý do rõ

  Database lỗi khi merge              Một schema SQL chuẩn dùng chung

  Tồn kho sai                         Test kỹ Confirm/Cancel/Return

  Giá sai                             BigDecimal + unitPrice snapshot

  Trễ tiến độ                         Cắt Observer/Decorator trước

  Thành viên vắng                     Module phải có README/ngắn gọn để
                                      người khác tiếp quản
  -----------------------------------------------------------------------

------------------------------------------------------------------------

# 31. Thứ tự ưu tiên khi thiếu thời gian

## P0 -- Bắt buộc

``` text
Login
Customer
Equipment
Rental
Inventory
State
Pricing
Singleton
Factory
Strategy
```

## P1 -- Nên có

``` text
JavaFX hoàn chỉnh
Search
Checkout
Payment Status
```

## P2 -- Có thời gian mới làm

``` text
Observer
Decorator
Dashboard
Advanced filter
Late fee
Damage/lost equipment
```

Nếu deadline gần, **cắt từ P2 xuống**, không cắt logic P0.

------------------------------------------------------------------------

# 32. Kịch bản demo cuối

Chuẩn bị sẵn một kịch bản duy nhất:

``` text
1. Login bằng admin.
2. Mở Customer.
3. Tạo/chọn Nguyễn Văn A.
4. Mở Equipment.
5. Cho thấy số lượng Lều = 5.
6. Tạo Rental cho Nguyễn Văn A.
7. Chọn Lều x1 + Ghế x2 + Đèn x1.
8. Chọn thời gian thuê.
9. Hệ thống tính giá.
10. Confirm đơn.
11. Kiểm tra availableQuantity đã giảm.
12. Chuyển Confirmed → Rented.
13. Cho thấy kho không bị giảm lần hai.
14. Chuyển Rented → Returned.
15. Kiểm tra kho được hoàn lại.
16. Xem Checkout/tổng tiền.
17. Xác nhận thanh toán.
```

Sau đó mỗi thành viên mở nhanh phần code Pattern của mình để giải thích.

------------------------------------------------------------------------

# 33. Câu hỏi mỗi thành viên phải trả lời được

1.  Module của bạn giải quyết nghiệp vụ gì?
2.  Pattern bạn sử dụng là gì?
3.  Pattern thuộc nhóm Creational, Structural hay Behavioral?
4.  Tại sao dùng pattern đó?
5.  Nếu không dùng pattern thì code gặp vấn đề gì?
6.  Các class nào tham gia pattern?
7.  Pattern giúp mở rộng hệ thống như thế nào?
8.  Module của bạn liên kết với module nào?
9.  Bạn kiểm thử module bằng trường hợp nào?
10. Phần nào do bạn trực tiếp triển khai?

------------------------------------------------------------------------

# 34. Deliverable cuối đề tài

Nhóm nên chuẩn bị:

``` text
Source code
SQL database/schema
README.md
Use Case Diagram
ERD
Class Diagram
Ảnh giao diện
Danh sách Design Pattern
Test cases
Báo cáo
Slide (nếu giảng viên yêu cầu)
```

Repository nên có:

``` text
camping-gear-rental/
|
+-- src/
+-- database/
|   +-- schema.sql
|   +-- seed.sql
|
+-- docs/
|   +-- use-case/
|   +-- erd/
|   +-- class-diagram/
|
+-- README.md
+-- pom.xml
+-- .gitignore
```

------------------------------------------------------------------------

# 35. Tiêu chí tự kiểm tra trước khi nộp

### Logic

-   [ ] Không overbooking.
-   [ ] Không tồn kho âm.
-   [ ] Confirm/Cancel/Return cập nhật kho đúng.
-   [ ] State transition đúng.
-   [ ] Giá đơn cũ không bị đổi.
-   [ ] Số ngày thuê không bằng 0.
-   [ ] Discount tính đúng.

### Code

-   [ ] Compile thành công.
-   [ ] Không còn lỗi console nghiêm trọng.
-   [ ] Không hard-code quá nhiều.
-   [ ] Pattern nằm đúng vị trí.
-   [ ] Controller không chứa toàn bộ business logic.
-   [ ] Không commit password database thật.

### Git

-   [ ] Main chạy được.
-   [ ] Branch đã merge.
-   [ ] Không còn conflict.
-   [ ] `.gitignore` đúng.
-   [ ] README hướng dẫn chạy rõ.

### Báo cáo

-   [ ] Tên đề tài thống nhất.
-   [ ] Use Case khớp chức năng.
-   [ ] ERD khớp database.
-   [ ] Class Diagram khớp code.
-   [ ] Pattern giải thích bằng chính code của nhóm.
-   [ ] Có phân công thành viên.
-   [ ] Có test case.
-   [ ] Có ảnh demo.

------------------------------------------------------------------------

# 36. Kết luận phạm vi

Phiên bản mục tiêu của nhóm là một **Java desktop prototype quản lý cho
thuê thiết bị cắm trại**, tập trung vào:

``` text
Customer
+
Equipment
+
Rental
+
Inventory
+
Pricing
+
Design Patterns
```

Bốn pattern chính:

``` text
Singleton
Factory
State
Strategy
```

Hai pattern tùy chọn:

``` text
Observer
Decorator
```

Nguyên tắc của nhóm:

> **Làm ít chức năng nhưng logic đúng, Design Pattern rõ ràng và demo ổn
> định tốt hơn làm nhiều chức năng nhưng không hoàn thiện.**
