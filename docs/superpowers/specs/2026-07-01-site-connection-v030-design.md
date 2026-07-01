# v0.3.0 Site Connection Design

## Goal

Build a site-ready version of the point shortage monitor that keeps connection credentials local, presents connection fields in Chinese for onsite operators, and queries the real site point table while preserving the readonly safety boundary.

## Decisions

- The GitHub public repository may contain the real business table and field names.
- The GitHub public repository must not contain real IP addresses, usernames, passwords, or real point group configurations.
- Users enter `host`, username, and password in the UI.
- Connection profiles are saved locally and can be reused on later launches.
- Passwords are not saved in v0.3.0. The password field remains runtime-only.
- The default PostgreSQL profile targets `database=cms_web`, `schema=public`, `port=2345`, `sslmode=disable`, with placeholder host/user values.
- The point monitoring query should use the site table `public.tcs_map_data`.
- The local H2 test database must remain available for offline validation and must mirror the site table shape closely enough for tests.

## UI Requirements

The connection management page must use Chinese labels:

- `连接ID`
- `连接名称`
- `数据库类型`
- `服务器地址/IP`
- `端口`
- `数据库名`
- `数据库空间/Schema`
- `用户名`
- `密码`
- `SSL模式`
- `本地测试库路径`

Buttons must use Chinese operator-facing text:

- `新建连接`
- `保存连接`
- `删除连接`
- `测试连接并使用`

Default PostgreSQL profile:

- connection name: `现场数据库`
- host: placeholder value such as `请填写现场IP`
- port: `2345`
- database: `cms_web`
- schema: `public`
- user: placeholder value such as `请填写用户名`
- sslmode: `disable`

## Data Source Requirements

The site point query maps columns from `public.tcs_map_data` into the existing `PointRecord` domain model:

| Site column | Domain meaning |
|---|---|
| `map_data_code` | point code |
| `pod_code` | shelf code |
| `pod_status` | shelf status |
| `status` | point status |
| `ind_lock` | lock state |
| `area_code` | area code |
| `relate_area_code` | related area code |
| `date_chg` | updated time |
| `date_cr` | fallback marked/created time |

Generated SQL must remain a parameterized `SELECT` with one `?` placeholder per point code. Schema identifiers must still pass the existing identifier whitelist.

## Readonly Boundary

No database writes are allowed. The application must continue to:

- open PostgreSQL connections through `ReadOnlyConnectionFactory`,
- issue `SET TRANSACTION READ ONLY`,
- use prepared statements for point queries,
- only write logs/configuration under local `data/` and `logs/`.

## Compatibility Requirements

The local H2 test database should create `public.tcs_map_data` with the site-compatible columns above. Tests that previously asserted `shelf_point_status` should be updated to the site table.

The database browser remains generic and can preview any allowed table selected by the user. The point shortage monitor is specifically tied to `tcs_map_data`.

## Documentation And Release

Update:

- `README.md`
- `CHANGELOG.md`
- `VERSION`
- `docs/releases/v0.3.0.md`
- `docs/manuals/point-shortage-alert-user-manual.md`
- packaged default `data/connections.properties`

The release package must not include real host, username, password, or real point groups.

## Validation

Required validation:

- full `.\build.ps1`
- self-test command
- source/document privacy scan for real IP, username, password, and known real point IDs
- packaged directory privacy scan
