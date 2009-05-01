#!/bin/sh

# Shell script to build the documentation, taken from the source .pod files.
 

if [ $# -gt 0 ]
then


  if [ "${1}" = "toc" ] ; then

    TOC="../src/001_TOC.pod";

    echo "Generating ${TOC}";

    # Remove the temporary TOC files if they exist:
    if [ -f .${TOC} ] ; then
        rm -f .${TOC};
    fi    
    if [ -f .${TOC}.tmp ] ; then
        rm -f .${TOC}.tmp;
    fi    

    # Extract headers from the pod documentation:
    for i in `ls -1 ../src/[0-9]*.pod | egrep -v "../src/0(0|1).*.pod"` ; do
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
    mv .${TOC} .${TOC};

  fi

  if [ -f ../src/docs.pod ] ; then
    rm -f ../src/docs.pod;
  fi    

  echo "Generating docs.pod";

  for i in ../src/[0-9]*.pod ; do
    cat $i >> ../src/docs.pod;
    echo -e "\n\n=ff\n\n" >> ../src/docs.pod;

  done

  if [ "${1}" = "pdf" ] ; then
    echo "Generating docs.pdf";
    pod2pdf ../src/docs.pod > ../output/docs.pdf;
  fi


  if [ "${1}" = "html" ] ; then
    echo "Generating docs.html";
    pod2html ../src/docs.pod > ../output/docs.html;
  fi

  if [ "${1}" = "all" ] ; then
    echo "Generating docs.pdf";
    pod2pdf ../src/docs.pod > ../output/docs.pdf;
    echo "Generating docs.html";
    pod2html ../src/docs.pod > ../output/docs.html;

  fi



  if [ "${1}" = "help" ] ; then
    echo ""
    echo "Using $0 to build the pod source documentation, and outputting as a choice of PDF, HTML, or just creating the TOC."
    echo "$0 toc  - Will build the TOC from the pod files"
    echo "$0 pdf  - Will build the documentaion and export all as a single PDF document."
    echo "$0 html - Will build the documentation and export all as a single html document."
    echo "$0 all - Will build the documentation and export all as a single html document and as a single PDF document."
    echo ""
    exit 2  

fi

 

 else 
  echo""
  echo "Incorrect usage: "
  echo "Use $0 (toc | pdf | html | all | help)"
  echo ""
  exit 3

 fi

exit 0;
