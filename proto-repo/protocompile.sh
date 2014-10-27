java -jar $1 --java_out=out *.proto 2> /tmp/log
zip -r $2 out
