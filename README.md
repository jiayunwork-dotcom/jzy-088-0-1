# BEM Service — 叶素动量气动核算 HTTP 服务

一副小型水平轴风机风轮的**叶素动量（Blade Element Momentum, BEM）气动核算**服务：
输入叶素序列（径向位置 / 弦长 / 扭角）、随攻角变化的升阻力系数表与叶尖速比，
逐叶素迭代求出轴向诱导因子 `a` 与切向诱导因子 `a'`，给出当地入流角、攻角、
微元推力、微元扭矩，并沿半径用梯形积分得到整机功率系数 `Cp` 与推力系数 `Ct`。

服务范围只到这一层气动核算：仅经 HTTP 返回 JSON，不含任何前端页面，也没有登录。

- Java 17 / Spring Boot 3.2
- 构建：Maven 3.9 + Temurin JDK 17（镜像 tag 用现成主线标签，不挑冷门 tag）
- 单容器即可对外提供服务（容器内监听 8080）

---

## 1. 气动方法

每个叶素做定点迭代（欠松弛）：

1. 由当前诱导因子算入流角
   `tan φ = (1 − a) / (λ·μ·(1 + a'))`，其中 `μ = r/R`；
2. 攻角 `α = φ − θ`，在翼型表中按攻角分段线性插值查 `Cl、Cd`（表外钳到端点）；
3. 法向 / 切向力系数 `Cn = Cl cosφ + Cd sinφ`、`Cy = Cl sinφ − Cd cosφ`；
4. 令 `K = σ Cn / sin²φ`（`σ = Bc/(2πr)`），更新诱导因子：
   - 小诱导（简单动量支，`K ≤ 8F/3`）：`a = K/(K + 4F)`；
   - 湍流尾流区（`a` 越过临界值 `a_c = 0.4`）改用 **Buhl (2005) 修正推力关系**
     `CT = 8/9 + (4F − 40/9)a + (50/9 − 4F)a²`，直接解关于 `a` 的二次方程，
     **绝不通过 `1/(1−a)` 反推**，分母不会在高诱导下失稳；
   - 切向：`a' = σ Cy (1−a)² / (4F sinφ cosφ − σ Cy (1−a)²)`，负值钳到 0；
5. **普朗特叶尖损失** `f = B(1−μ)/(2μ sinφ)`、`F = (2/π) arccos(e⁻ᶠ)`
   贯穿全部动量关系（可通过 `tipLoss:false` 关闭）；
6. 欠松弛后重复，直到 `max(|Δa|, |Δa'|) < 1e-6`。超过 200 次或 `a` 越出物理
   区间即判**不收敛并报错**（HTTP 422，`ITERATION_NOT_CONVERGED`），
   不会返回看似有值、实则发散的结果。

沿叶片（梯形积分，对圆盘面积归一化）：

```
Ct = (1/π) ∫ dT* dμ
Cp = (λ/π) ∫ dQ* dμ
```

物理单位由 `q = ½ρU²` 和半径 `R` 恢复：`T = qR∫dT*dμ`、
`Q = qR²∫dQ*dμ`、`P = Qω`，`ω = λU/R`。

### 模块划分（八块各自独立成文件，内核与积分不堆在一个类里）

| # | 职责 | 文件 |
|---|------|------|
| 1 | 入流角 / 攻角求解 | `aero/FlowAngles.java` |
| 2 | 翼型表插值 | `aero/PolarInterpolator.java` |
| 3 | 诱导因子迭代 | `aero/InductionSolver.java` |
| 4 | 尾流修正（Buhl） | `aero/WakeModel.java` |
| 5 | 普朗特叶尖损失 | `aero/PrandtlTipLoss.java` |
| 6 | 沿叶片积分 | `aero/BladeIntegration.java` |
| 7 | 翼型数据登记与管理 | `airfoil/AirfoilRegistry.java`、`airfoil/InMemoryAirfoilRegistry.java` |
| 8 | 输入校验 | `validation/RotorRequestValidator.java`、`validation/ApiException.java`、`validation/ErrorCode.java` |

HTTP 层在 `web/`，服务编排在 `service/BemService.java`，二者都不含迭代公式。

---

## 2. HTTP 能力

基址 `http://localhost:8080`。两个气动能力：

### 2.1 单个叶素 + 叶尖速比 → 该站诱导因子 + 整机系数

`POST /api/bem/station`

```json
{
  "blades": 3,
  "tipSpeedRatio": 7.0,
  "airfoilName": "naca4412-sample",
  "elements": [ ... 整片叶片，至少 3 站，按 r/R 递增 ... ],
  "stationROverR": 0.63824,
  "radiusM": 2.0,
  "freeStreamMs": 8.0,
  "densityKgM3": 1.225,
  "tipLoss": true
}
```

> 整机 `Cp/Ct` 必须沿全叶片积分，所以仍要提交整片叶片；
> `stationROverR` 选出要突出的那一站（省略时取最外站）。
> 返回该站 `axialInduction`、`tangentialInduction`、`inflowDeg`、
> `angleOfAttackDeg`、`cl/cd`、`tipLossFactor`，以及整机 `powerCoefficient`、
> `thrustCoefficient`、`thrustN`、`torqueNm`、`powerW`。

### 2.2 整片叶片 → 各半径攻角 / 微元推力 / 微元扭矩

`POST /api/bem/rotor`，请求体同上（不需要 `stationROverR`）。
返回 `stations[]`：每站含 `angleOfAttackDeg`、`dThrustPerSpan`（N/m）、
`dTorquePerSpan`（N·m/m）、诱导因子等，外加整机积分量。

### 翼型数据登记（具名 polar，供多次分析引用）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/airfoils` | 登记具名 polar（`{name, points:[{alphaDeg,cl,cd}, ...]}`，点须按攻角严格递增） |
| GET  | `/api/airfoils` | 列出全部 |
| GET  | `/api/airfoils/{name}` | 取一张 |
| DELETE | `/api/airfoils/{name}` | 删除 |

### 内置三叶片算例

- `GET /api/example?tipSpeedRatio=7`：返回算例叶片定义与所引用的翼型名；
- `POST /api/example/analyze?tipSpeedRatio=7&tipLoss=true`：直接核算。

服务启动时自动登记翼型 `naca4412-sample`（NACA 4412 风格 polar，约 Re 1e6）。
算例为 3 叶片、R = 2 m、设计 λ = 7、18 站（μ = 0.15…0.98），按理想（Betz）
设计关系配弦长 / 扭角（设计 Cl = 1、设计攻角 6°）。

**设计点可手工核对的结果（λ = 7，U = 8 m/s，ρ = 1.225）：**

| 量 | 值 |
|----|----|
| `Cp` | **0.489**（正且 < Betz 16/27 = 0.5926） |
| `Ct` | 0.818 |
| 叶中区 `a` | ≈ 0.33（设计值 1/3） |
| 叶中区攻角 | ≈ 6.0°（设计值） |
| 功率 `P` | ≈ 963 W |
| 推力 `T` | ≈ 202 N |

λ 扫描：Cp 从低速端随 λ 上升（λ=3 约 0.15），在 λ≈6.5–7.5 形成 ~0.49 平台，
之后因偏离设计点而下降，全程不超过 Betz 极限。

### 错误响应

非法输入返回统一结构，带机器码、可读说明与 HTTP 状态：

```json
{ "code": "TIP_SPEED_RATIO_NOT_POSITIVE",
  "message": "tip speed ratio must be a positive number, got: -1.0",
  "status": 400, "time": "2026-..." }
```

| 机器码 | 触发条件 | HTTP |
|--------|----------|------|
| `TIP_SPEED_RATIO_NOT_POSITIVE` | 叶尖速比不为正 | 400 |
| `NOT_ENOUGH_STATIONS` | 径向站点少于 3 个 | 400 |
| `AIRFOIL_TABLE_EMPTY` | 翼型系数表为空 | 400 |
| `RADIUS_NOT_MONOTONIC` | 径向坐标非严格递增 | 400 |
| `BLADES_NOT_POSITIVE_INTEGER` | 叶片数不为正整数 | 400 |
| `RADIUS_OUT_OF_RANGE` / `CHORD_NOT_POSITIVE` | r/R 不在 (0,1]、弦长非正 | 400 |
| `POLAR_NOT_ORDERED` / `POLAR_POINT_INVALID` | polar 未按攻角递增 / 点缺字段 | 400 |
| `AIRFOIL_NAME_MISSING` / `AIRFOIL_NOT_FOUND` | 未给名 / 名字未登记 | 400 / 404 |
| `AIRFOIL_ALREADY_EXISTS` | 重复登记 | 409 |
| `STATION_NOT_FOUND` | 指定站点在叶片上不存在 | 404 |
| `ITERATION_NOT_CONVERGED` | 迭代不收敛 / 发散 | 422 |
| `INVALID_JSON` | 请求体不是合法 JSON | 400 |

---

## 3. 本地构建与运行

```bash
mvn clean package          # 编译 + 44 个测试
java -jar target/bem-service-1.0.0.jar
```

## 4. 容器化（单容器对外）

```bash
docker build -t bem-service:1.0.0 .
docker run --rm -p 8080:8080 bem-service:1.0.0
# 试一下：
curl -X POST "http://localhost:8080/api/example/analyze?tipSpeedRatio=7"
```

Dockerfile 为多阶段：构建阶段 `maven:3.9-eclipse-temurin-17`，
运行阶段 `eclipse-temurin:17-jre`，非 root 用户运行，内建 HEALTHCHECK。

## 5. 测试

```bash
mvn test
```

44 个测试全部带显式容差，覆盖：

- 非法输入五类（λ 非正、站点 < 3、空翼型表、半径非单调、叶片数非正）及其他校验码；
- 收敛后叶中区轴向诱导因子落在 (0, 1/2)；
- `0 < Cp ≤ 16/27`（含整个 λ 扫描区间）；
- λ 从偏低抬向设计点，Cp **先上升、再在设计点附近平台**，而非单调乱跳；
- 关闭叶尖损失后，**梢部微元推力高于**计入损失时；
- 其它参数不变、叶片数 3→2→1，Cp **明显下降**；
- Buhl 两支在 `a_c=0.4` 处值与斜率连续、高推力支不返回 a=1；
- 迭代预算被压到不可能收敛时，抛 `ConvergenceException` 而非返回发散值；
- HTTP 层两个能力、错误契约、翼型登记生命周期、内置算例（MockMvc 端到端）。
