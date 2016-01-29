java -jar $1 --java_out=out --files=protos.include 2> /tmp/log
zip -r $2 out
