package ca.sqlpower.sql;

public class SQL {

    /**
     * This class cannot be instantiated
     */
    private SQL()
    {
    }

    /**
     * A convent way of using escapeStatement.  This method does the
     * same thing as escapeStatement, but also encloses the returned
     * string in single-quotes.
     *
     * @param string The string that you want escaped and quoted.
     * @return The same as escapeStatement would, but enclosed in quotes.
     */
    public static String quote(String string) {
	return "'"+escapeStatement(string)+"'";
    }

    /**
     * Makes the input string safe to enclose in single-quotes in an
     * Oracle SQL expression.  Currently, this only means turning all
     * "'" characters into the "''" escape sequence.
     *
     * @param old The original string
     * @return The awk/perl substitution on <code>old</code> of
     * <code>s/'/''/g</code>
     */
    public static String escapeStatement(String old) 
    { 
	if(old==null) {
	    return "null";
	}

	// a premature optimisation 
	if(old.lastIndexOf('\'') == -1) { 
	    return old; 
	} 
	
	int i=0; 
	StringBuffer escaped=new StringBuffer(old); 
	
	while(i < escaped.length()) 
	    { 
		if(escaped.charAt(i)=='\'') { 
		    escaped.insert(i, '\''); 
		    i++;  // skip over the added quote 
		} 
		i++; 
	    } 
	return(escaped.toString()); 
    } 

    /**
     * Converts the character representation of a YES/NO value into
     * boolean.
     *
     * @param indicator A string that says YES or NO or Y or N.
     * @return true iff <code>indicator.charAt(0)=='Y'</code>.
     */
    public static boolean decodeInd(String indicator) {
	if(indicator.charAt(0) == 'Y') {
	    return true;
	}
	return false;
    }
}
