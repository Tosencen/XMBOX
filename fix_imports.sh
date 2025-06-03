#!/bin/bash

# 修复ViewBinding import路径的脚本

echo "开始修复ViewBinding import路径..."

# 修复Java文件中的databinding import
find app/src/main/java -name "*.java" -exec sed -i '' 's/import com\.github\.tvbox\.osc\.databinding\./import com.xmbox.app.databinding./g' {} \;

# 修复Kotlin文件中的databinding import
find app/src/main/java -name "*.kt" -exec sed -i '' 's/import com\.github\.tvbox\.osc\.databinding\./import com.xmbox.app.databinding./g' {} \;

# 修复Java文件中的R import
find app/src/main/java -name "*.java" -exec sed -i '' 's/import com\.github\.tvbox\.osc\.R;/import com.xmbox.app.R;/g' {} \;

# 修复Kotlin文件中的R import
find app/src/main/java -name "*.kt" -exec sed -i '' 's/import com\.github\.tvbox\.osc\.R/import com.xmbox.app.R/g' {} \;

# 修复Java文件中的databinding完整路径引用
find app/src/main/java -name "*.java" -exec sed -i '' 's/com\.github\.tvbox\.osc\.databinding\./com.xmbox.app.databinding./g' {} \;

echo "修复完成！"

# 显示修改的文件
echo "已修改的文件："
find app/src/main/java -name "*.java" -o -name "*.kt" | xargs grep -l "com.xmbox.app.databinding\|com.xmbox.app.R" | head -20
