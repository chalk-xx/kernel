#!/bin/sh
#!/bin/sh
TESTS="authz-test.rb contact-test.rb message-test.rb node-create-test.rb owner-principal-test.rb sling-search-test.rb sling-site-test.rb user-manager-test.rb file-test.rb private-store-test.rb "
for i in $TESTS
do
echo $i `./$i | grep failure`
done
