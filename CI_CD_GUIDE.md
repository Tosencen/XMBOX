# CI/CD 使用指南

本项目使用GitHub Actions自动化构建和发布Android应用。以下是各个工作流的说明和使用方法。

## 可用的工作流

### 1. Android Build (android-build.yml)

**功能**：自动构建Debug版本的APK

**触发条件**：
- 向main或master分支推送代码
- 创建针对main或master分支的Pull Request
- 手动触发

**使用方法**：
- 自动触发：只需推送代码到main或master分支
- 手动触发：
  1. 在GitHub仓库页面，点击"Actions"标签
  2. 在左侧选择"Android Build"工作流
  3. 点击"Run workflow"按钮
  4. 选择分支，然后点击"Run workflow"

**构建结果**：
- 构建完成后，可以在Actions执行页面下载生成的APK文件

### 2. Android Release (android-release.yml)

**功能**：构建Release版本的APK并创建GitHub Release

**触发条件**：
- 推送以"v"开头的标签，例如v1.0.0

**使用方法**：
```bash
# 创建新标签
git tag v1.0.0

# 推送标签到GitHub
git push origin v1.0.0
```

**构建结果**：
- 构建完成后，会自动创建一个新的GitHub Release
- Release中会包含构建好的APK文件

### 3. Resource Check (resource-check.yml)

**功能**：检查资源文件名是否符合Android命名规范

**触发条件**：
- 向main或master分支推送代码
- 创建针对main或master分支的Pull Request
- 手动触发

**使用方法**：
- 自动触发：只需推送代码到main或master分支
- 手动触发：同Android Build工作流

**检查结果**：
- 工作流会输出所有不符合命名规范的资源文件
- 目前只会提示，不会自动修改文件名
- 如需启用自动修复，请编辑`.github/workflows/resource-check.yml`文件，取消相应注释

## 配置签名

如果需要构建已签名的Release版本，请按以下步骤配置：

1. 在GitHub仓库设置中添加以下Secrets：
   - `SIGNING_KEY`：Base64编码的签名密钥文件
   - `ALIAS`：密钥别名
   - `KEY_STORE_PASSWORD`：密钥库密码
   - `KEY_PASSWORD`：密钥密码

2. 编辑`.github/workflows/android-release.yml`文件，取消Sign APK步骤的注释

## 自定义构建配置

如需自定义构建配置，可以编辑相应的工作流文件：

- `.github/workflows/android-build.yml`
- `.github/workflows/android-release.yml`
- `.github/workflows/resource-check.yml`

## 常见问题

### Q: 如何查看构建日志？
A: 在GitHub仓库页面，点击"Actions"标签，选择相应的工作流执行记录，即可查看详细日志。

### Q: 构建失败怎么办？
A: 查看构建日志，找出失败原因。常见的失败原因包括：
- 代码错误
- 资源文件命名不规范
- Gradle配置问题

### Q: 如何禁用某个工作流？
A: 在GitHub仓库设置中，点击"Actions"，然后可以选择禁用特定的工作流。
