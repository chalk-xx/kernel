#!/bin/sh

if [ "${1}" = "-t" ] ; then

    TOC="01_TOC.pod";

    echo "Generating ${TOC}";

    # Remove the temporary TOC files if they exist:
    if [ -f .${TOC} ] ; then
        rm -f .${TOC};
    fi    
    if [ -f .${TOC}.tmp ] ; then
        rm -f .${TOC}.tmp;
    fi    

    # Extract headers from the pod documentation:
    for i in `ls -1 [0-9]*.pod | egrep -v "0(0|1).*.pod"` ; do
        grep "=head" $i >> .${TOC};
    done

    # Add in list starter (=over) and finisher (=back):
    last_head="";
    cat .${TOC} | while read i ; do
        head=`echo $i | cut -d' ' -f1`;
        if [ "${head}" = "=head1" ] ; then
            if [ "${last_head}" != "" -a "${last_head}" != "=head1" ] ; then
	        echo "=back" >> .${TOC}.tmp;
	    fi
        else
            if [ "${last_head}" = "=head1" ] ; then
	        echo "=over" >> .${TOC}.tmp;
	    fi
        fi
        echo "${i}" >> .${TOC}.tmp;
        last_head="${head}";
    done
    mv .${TOC}.tmp .${TOC};

    # Convvert sub-headings to list items:
    perl -pe 's/=head[2-9](.*)/=item$1/g' .${TOC} > .${TOC}.tmp;
    mv .${TOC}.tmp .${TOC};
    # Convert main headings to headings:
    perl -pe 's/=head1(.*)/=head3$1 ... Page 1/g' .${TOC} > .${TOC}.tmp;
    mv .${TOC}.tmp .${TOC};


    # Add a header to the file:
    echo "=head1 The Next Generation of Sakai. A reference guide:" > .${TOC}.tmp;
    echo "=head2 Table Of Contents" >> .${TOC}.tmp;
    cat .${TOC} >> .${TOC}.tmp;
    mv .${TOC}.tmp .${TOC};

    # Add a newline to every line:
    perl -pe 's/$/\n/' .${TOC} > .${TOC}.tmp;
    mv .${TOC}.tmp .${TOC};
    mv .${TOC} ${TOC};

fi

if [ -f documentation.pod ] ; then
    rm -f documentation.pod;
fi    

echo "Generating documentation.pod";

for i in [0-9]*.pod ; do
    cat $i >> documentation.pod;
    echo -e "\n\n=ff\n\n" >> documentation.pod;
done

echo "Generating documentation.pdf";

pod2pdf documentation.pod > documentation.pdf;

exit 0;
