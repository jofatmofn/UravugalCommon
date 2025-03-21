package org.sakuram.relation.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Constants {
	
	public static final byte ENTITY_TYPE_PERSON = (byte)1;
	public static final byte ENTITY_TYPE_RELATION = (byte)2;
	
	public static final String CATEGORY_RELATION_NAME = "RelName";
	public static final String CATEGORY_RELATION_SUB_TYPE = "RelSubType";
	public static final String CATEGORY_PERSON_ATTRIBUTE = "PersAttribute";
	public static final String CATEGORY_ADDITIONAL_PERSON_ATTRIBUTE = "AddlPersAttribute";
	public static final String CATEGORY_RELATION_ATTRIBUTE = "RelAttribute";
	public static final String CATEGORY_LANGUAGE = "Language";

	public static final long PERSON_ATTRIBUTE_DV_ID_LABEL = 16;
	public static final long PERSON_ATTRIBUTE_DV_ID_GENDER = 19;
	public static final long PERSON_ATTRIBUTE_DV_ID_FIRST_NAME = 20;
	public static final long PERSON_ATTRIBUTE_DV_ID_MIDDLE_NAME = 21;
	public static final long PERSON_ATTRIBUTE_DV_ID_SUR_NAME = 22;
	public static final long PERSON_ATTRIBUTE_DV_ID_NICK_NAME = 314;
	public static final long PERSON_ATTRIBUTE_DV_ID_NAME_PREFIX = 338;
	public static final long PERSON_ATTRIBUTE_DV_ID_MANAGE_USER = 353;
	public static final long PERSON_ATTRIBUTE_DV_ID_PERSON_ID = 400;
	public static final long PERSON_ATTRIBUTE_DV_ID_PARENTS = 401;
	public static final long PERSON_ATTRIBUTE_DV_ID_SPOUSES = 402;
	public static final long PERSON_ATTRIBUTE_DV_ID_CHILDREN = 403;
	public static final long PERSON_ATTRIBUTE_DV_ID_SIBLINGS = 404;
	public static final long PERSON_ATTRIBUTE_DV_ID_ANY_NAME = 405;
	public static final long[] PERSON_ATTRIBUTE_DV_IDS_ARRAY_NAME = {PERSON_ATTRIBUTE_DV_ID_LABEL, PERSON_ATTRIBUTE_DV_ID_FIRST_NAME, PERSON_ATTRIBUTE_DV_ID_MIDDLE_NAME, 
			PERSON_ATTRIBUTE_DV_ID_SUR_NAME, PERSON_ATTRIBUTE_DV_ID_NICK_NAME, PERSON_ATTRIBUTE_DV_ID_NAME_PREFIX};
	public static final long RELATION_ATTRIBUTE_DV_ID_PERSON1_FOR_PERSON2 = 34;
	public static final long RELATION_ATTRIBUTE_DV_ID_PERSON2_FOR_PERSON1 = 35;
	public static final long RELATION_ATTRIBUTE_DV_ID_SEQUENCE_OF_PERSON1_FOR_PERSON2 = 61;
	public static final long RELATION_ATTRIBUTE_DV_ID_SEQUENCE_OF_PERSON2_FOR_PERSON1 = 62;
	public static final long RELATION_ATTRIBUTE_DV_ID_RELATION_SUB_TYPE = 36;
	public static final long RELATION_ATTRIBUTE_DV_ID_YYYY_OF_RELATION_END = 351;
	public static final long RELATION_ATTRIBUTE_DV_ID_IS_AUTO_GENERATED = 414;
	public static final long OTHER_RELATION_TYPE_DV_ID_SIBLING = 408;
	public static final long OTHER_RELATION_TYPE_DV_ID_COUSIN_BROTHER_SISTER = 409;
	public static final long OTHER_RELATION_TYPE_DV_ID_COUSIN = 410;
	public static final long OTHER_RELATION_VIA_DV_ID_FATHER = 411;
	public static final long OTHER_RELATION_VIA_DV_ID_MOTHER = 412;
	public static final long OTHER_RELATION_VIA_DV_ID_NOT_KNOWN = 413;
	
	public static final String RELATION_NAME_FATHER = "1";
	public static final String RELATION_NAME_MOTHER = "2";
	public static final String RELATION_NAME_HUSBAND = "3";
	public static final String RELATION_NAME_WIFE = "4";
	public static final String RELATION_NAME_SON = "5";
	public static final String RELATION_NAME_DAUGHTER = "6";
	public static final List<String> RELATION_NAME_LIST_PARENT = Arrays.asList(RELATION_NAME_FATHER, RELATION_NAME_MOTHER);
	public static final List<String> RELATION_NAME_LIST_SPOUSE = Arrays.asList(RELATION_NAME_HUSBAND, RELATION_NAME_WIFE);
	public static final List<String> RELATION_NAME_LIST_CHILD = Arrays.asList(RELATION_NAME_SON, RELATION_NAME_DAUGHTER);
	public static final String RELATION_NAME_BROTHER = "316";
	public static final String RELATION_NAME_SISTER = "317";
	public static final String RELATION_NAME_FATHER_IN_LAW = "318";
	public static final String RELATION_NAME_MOTHER_IN_LAW = "319";
	public static final String RELATION_NAME_SON_IN_LAW = "320";
	public static final String RELATION_NAME_DAUGHTER_IN_LAW = "321";
	public static final String RELATION_NAME_UNCLE = "324";
	public static final String RELATION_NAME_AUNT = "325";
	public static final String RELATION_NAME_NEPHEW = "326";
	public static final String RELATION_NAME_NIECE = "327";
	public static final String RELATION_NAME_COUSIN_BROTHER = "328";
	public static final String RELATION_NAME_COUSIN_SISTER = "329";
	public static final String RELATION_NAME_COUSIN_MALE = "330";
	public static final String RELATION_NAME_COUSIN_FEMALE = "331";
	public static final String GENDER_NAME_MALE = "59";
	public static final String GENDER_NAME_FEMALE = "60";
	public static final String BOOL_TRUE = "41";
	public static Map<String, String> RELATION_NAME_TO_ID_MAP = Stream.of(new String[][] {
		  { "Father", "1" },
		  { "Mother", "2" },
		  { "Husband", "3" },
		  { "Wife", "4" },
		  { "Son", "5" },
		  { "Daughter", "6" },
		  { "Brother", "316" },
		  { "Sister", "317" },
		  { "Father-in-law", "318" },
		  { "Mother-in-law", "319" },
		  { "Son-in-law", "320" },
		  { "Daughter-in-law", "321" },
		  { "Uncle", "324" },
		  { "Aunt", "325" },
		  { "Nephew", "326" },
		  { "Niece", "327" },
		  { "Cousin-brother", "328" },
		  { "Cousin-sister", "329" },
		  { "Cousin-male", "330" },
		  { "Cousin-female", "331" },
		  }).collect(Collectors.toMap(data -> data[0], data -> data[1]));	// TODO: Read it from DB?
	public static final String FLAG_RELATION_GROUP_PARENT_CHILD = "PC";
	public static final String FLAG_RELATION_GROUP_SPOUSE = "Sp";
	
	public static final String FLAG_ATTRIBUTE_DOMAIN_LANGUAGE = "Language";
	public static final String FLAG_ATTRIBUTE_DOMAIN_CITY = "City";
	public static final String FLAG_ATTRIBUTE_DOMAIN_COUNTRY = "Country";
	public static final String FLAG_ATTRIBUTE_UI_INPUT = "INP";
	public static final String FLAG_ATTRIBUTE_UI_INTERNAL = "INT";
	public static final String FLAG_ATTRIBUTE_UI_DISPLAY = "DSP";
	public static final String FLAG_ATTRIBUTE_REPETITION_NOT_ALLOWED = "NA";
	public static final String FLAG_ATTRIBUTE_REPETITION_OVERLAPPING_ALLOWED = "OA";
	public static final String FLAG_ATTRIBUTE_REPETITION_NON_OVERLAPPING_ALLOWED = "NOA";
	public static final String FLAG_ATTRIBUTE_PRIVACY_RESTRICTION_PUBLIC_ONLY = "PB";
	public static final String FLAG_ATTRIBUTE_PRIVACY_RESTRICTION_PRIVATE_ONLY = "PR";
	public static final String FLAG_ATTRIBUTE_PRIVACY_RESTRICTION_INDIVIDUAL_CHOICE = "BR";
	public static final String FLAG_ATTRIBUTE_SCRIPT_CONVERSION_TRANSLATE = "A";
	public static final String FLAG_ATTRIBUTE_SCRIPT_CONVERSION_TRANSLITERATE = "I";

	public static final int FLAG_POSITION_RELATION_GROUP = 0;
	public static final int FLAG_POSITION_INPUT_AS_ATTRIBUTE = 0;
	public static final int FLAG_POSITION_REPETITION = 1;
	public static final int FLAG_POSITION_DOMAIN = 2;
	public static final int FLAG_POSITION_INPUT_MANDATORY = 3;
	public static final int FLAG_POSITION_VALIDATION_JS_REG_EX = 4;
	public static final int FLAG_POSITION_PRIVACY_RESTRICTION = 5;
	public static final int FLAG_POSITION_REL_ATTR_APPLICABLE_REL_GROUP = 6;
	public static final int FLAG_POSITION_SCRIPT_CONVERSION = 7;
	public static final int FLAG_POSITION_SEARCH_RESULT_COLUMN_INDEX = 8;
	public static final int FLAG_POSITION_ISO_LANGUAGE_CODE = 0;

	public static final String EDGE_TYPE_DIRECT_RELATION = "line";
	public static final String EDGE_TYPE_SIMPLIFIED_RELATION = "curve";
	
	public static final String CSV_SEPARATOR = ",";
	public static final long NEW_ENTITY_ID = -1L;
	public static final String DEFAULT_NODE_COLOR = "rgb(1,179,255)";
	public static final String DEFAULT_EDGE_COLOR = "rgb(1,179,255)";
	public static final double DEFAULT_NODE_SIZE = 5.0;
	public static final double DEFAULT_EDGE_SIZE = 2.0;
	public static final long DELETED_ATTRIBUTE_VALUE_ID = -1L;
	public static final int SEARCH_RESULTS_MAX_COUNT = 20;
	public static final String RELATION_LABEL_TEMPLATE = "@@person1@@:@@34@@(@@61@@)-@@person2@@:@@35@@(@@62@@)(@@36@@)"; // TODO: Replace the hard-coded value with Constant
	public static final short EXPORT_TREE_MAX_DEPTH = 10;
	public static final short EXPORT_TREE_MAX_ROWS = 0;
	public static final short EXPORT_TREE_MAX_PERSONS = 0;
	public static final long DEFAULT_LANGUAGE_DV_ID = 336;
	public static final long ENGLISH_LANGUAGE_DV_ID = 336;
	public static final long TAMIL_LANGUAGE_DV_ID = 51;
	public static final String UPLOAD_FUNCTION_CHECK_DUPLICATES = "ufCheckDuplicates";
	public static final String UPLOAD_FUNCTION_STORE = "ufStore";
	public static final String EXPORT_TREE_TYPE_FULL = "exportFullTree";
	
	public static final String SESSION_ATTRIBUTE_PROJECT_SURROGATE_ID = "projectId";
	public static final String SESSION_ATTRIBUTE_USER_SURROGATE_ID = "userId";
	public static final String SESSION_ATTRIBUTE_LANGUAGE_DV_ID = "languageDvId";
	
	public static final long ROLE_DV_ID_CREATOR = 332;
	public static final long ROLE_DV_ID_COLLABORATOR = 333;
	
	public static final int UPLOAD_CELL_STRUCTURE_MIN_LEN = 2;
	public static final int UPLOAD_CELL_STRUCTURE_MAX_LEN = 6;
	public static final int UPLOAD_CELL_STRUCTURE_POSITION_SEQNO = 0;
	public static final int UPLOAD_CELL_STRUCTURE_POSITION_FIRST_NAME = 1;
	public static final int UPLOAD_CELL_STRUCTURE_POSITION_REFER_TO = 1;
	public static final int UPLOAD_CELL_STRUCTURE_POSITION_GENDER = 2;
	public static final int UPLOAD_CELL_STRUCTURE_POSITION_SUR_NAME = 3;
	public static final int UPLOAD_CELL_STRUCTURE_POSITION_LABEL = 4;
	public static final int UPLOAD_CELL_STRUCTURE_POSITION_REFERRED_AS = 5;
	
}
