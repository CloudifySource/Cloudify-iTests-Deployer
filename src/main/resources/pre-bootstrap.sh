echo starting pre-bootstrap...

#setting ulimit -n and -u 
if [ ! -z $CLOUDIFY_OPEN_FILES_LIMIT ] 
	then
		echo setting hard and soft open files ulimit to $CLOUDIFY_OPEN_FILES_LIMIT
		ulimit -HSn $CLOUDIFY_OPEN_FILES_LIMIT
		ulimit -HSu $CLOUDIFY_OPEN_FILES_LIMIT
		echo Finished setting open files limit
	
fi