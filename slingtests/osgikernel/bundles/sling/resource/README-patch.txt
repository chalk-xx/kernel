This is a patched version of the Slign bundle/jcr/resource bundle. If that code changes, the update mechanims is 

for i in `find . -name '*.java'`
do
  cp $SLING_SOURCE/$i $i
done
patch -p9 < SLING-1129.diff
