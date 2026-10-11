import zipfile

def rename_class_package(jar_path, output_path, old_package, new_package):
    """Rename package in a JAR file by modifying class files."""
    old_slash = old_package.replace('.', '/')
    new_slash = new_package.replace('.', '/')
    old_bytes = old_package.encode('utf-8')
    new_bytes = new_package.encode('utf-8')
    old_slash_bytes = old_slash.encode('utf-8')
    new_slash_bytes = new_slash.encode('utf-8')
    
    with zipfile.ZipFile(jar_path, 'r') as zin:
        with zipfile.ZipFile(output_path, 'w') as zout:
            for item in zin.infolist():
                data = zin.read(item.filename)
                # Replace package name in class file (both slash and dot formats)
                data = data.replace(old_slash_bytes, new_slash_bytes)
                data = data.replace(old_bytes, new_bytes)
                # Also fix the filename in the zip
                new_filename = item.filename.replace(old_slash, new_slash)
                zout.writestr(new_filename, data)

if __name__ == '__main__':
    rename_class_package(
        '/home/runner/work/mcJava/mcJava/android/app/build/match-exception.jar',
        '/home/runner/work/mcJava/mcJava/android/app/build/match-exception-patched.jar',
        'com.patched.java.lang',
        'java.lang'
    )
    print("Done")
