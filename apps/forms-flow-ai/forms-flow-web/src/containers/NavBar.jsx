import React, { useEffect, useMemo, useState } from "react";
import { Navbar, Container, Nav, NavDropdown } from "react-bootstrap";
import {Link, useLocation} from "react-router-dom";
import {useDispatch, useSelector} from "react-redux";
import UserService from "../services/UserService";
import {
  getUserRoleName, 
  getUserRolePermission, 
  getUserInsightsPermission
} from "../helper/user";
import createURLPathMatchExp from "../helper/regExp/pathMatch";
import { useTranslation } from "react-i18next";

import "./styles.scss";
import {CLIENT, STAFF_REVIEWER, APPLICATION_NAME, STAFF_DESIGNER, MULTITENANCY_ENABLED,} from "../constants/constants";
import ServiceFlowFilterListDropDown from "../components/ServiceFlow/filter/ServiceTaskFilterListDropDown";
import { push } from "connected-react-router";
import i18n from "../resourceBundles/i18n";
import { setLanguage } from "../actions/languageSetAction";
import { updateUserlang } from "../apiManager/services/userservices";

import { fetchSelectLanguages } from "../apiManager/services/languageServices";

const NavBar = React.memo(() => {
  const isAuthenticated = useSelector((state) => state.user.isAuthenticated);
  const location = useLocation();
  const { pathname } = location;
  const user = useSelector((state) => state.user.userDetail);
  const lang = useSelector((state) => state.user.lang);
  const userRoles = useSelector((state) => state.user.roles);
  const showApplications= useSelector((state) => state.user.showApplications);
  const applicationTitle = useSelector(
    (state) => state.tenants?.tenantData?.details?.applicationTitle
  );
  const tenantKey = useSelector((state) => state.tenants?.tenantId);
  const formTenant = useSelector((state)=>state.form?.form?.tenantKey);
  const baseUrl = MULTITENANCY_ENABLED ? `/tenant/${tenantKey}/` : "/";


  /**
   * For anonymous forms the only way to identify the tenant is through the
   * form data with current implementation. To redirect to the correact tenant
   * we will use form as the data source for the tenantKey
   */

  const [loginUrl, setLoginUrl] = useState(baseUrl);

  const selectLanguages = useSelector((state) => state.user.selectLanguages);
  const dispatch = useDispatch();
  const logoPath = "/logo.svg";
  const getAppName = useMemo(
    () => () => {
      if (!MULTITENANCY_ENABLED) {
        return APPLICATION_NAME;
      }
      // TODO: Need a propper fallback component prefered a skeleton.
      return applicationTitle || "";
    },
    [MULTITENANCY_ENABLED, applicationTitle]
  );
  const appName = getAppName();
  const { t } = useTranslation();

  useEffect(()=>{
    if(!isAuthenticated && formTenant && MULTITENANCY_ENABLED){ 
      setLoginUrl(`/tenant/${formTenant}/`);
    }
  },[isAuthenticated, formTenant]);

  useEffect(() => {
    dispatch(fetchSelectLanguages());
  }, [dispatch]);

  useEffect(() => {
    i18n.changeLanguage(lang);
  }, [lang]);

  const handleOnclick = (selectedLang) => {
    dispatch(setLanguage(selectedLang));
    dispatch(updateUserlang(selectedLang));
  };

  const logout = () => {
    dispatch(push(baseUrl));
    UserService.userLogout();
  };

  const goToTask = () => {
    dispatch(push(`${baseUrl}task`));
  };
  return (
    <header>
      <Navbar collapseOnSelect expand="lg" className="topheading-border-bottom" fixed="top">
        <Container fluid className="service-bc-navbar-background">
          {/*<Nav className="d-lg-none">
            <div className="mt-1" onClick={menuToggle}>
              <i className="fa fa-bars fa-lg"/>
            </div>
          </Nav>*/}
          <Navbar.Brand className="d-flex" >
            <Link to={`${baseUrl}`}>
              <img
                className="img-fluid"
                src={logoPath}
                width="100px"
                min-width="87px"
                max-width="100px"
                max-height="55px"
                alt="Logo"
              />
            </Link>
            <div className="custom-app-name pl-2">{appName}</div>
          </Navbar.Brand>
         {/*
           <Navbar.Brand className="d-flex">
            <Link to="/">
                  <img
                    className="img-xs rounded-circle"
                    src="/assets/Images/user.svg"
                    alt="profile"
                  />
            </Link>
          </Navbar.Brand>*/}
          <Navbar.Toggle aria-controls="responsive-navbar-nav" />
         { isAuthenticated ? 
            <Navbar.Collapse id="responsive-navbar-nav" className="navbar-nav">
            <Nav id="main-menu-nav" className="mr-auto">
              <Nav.Link eventKey="1" as={Link} to={`${baseUrl}form`}  className={`main-nav nav-item ${
                pathname.match(createURLPathMatchExp("form", baseUrl)) ? "" : "inactive-tab"
              }`}>  
                <i
                  className={`fa fa-wpforms fa-lg mr-2
                      ${pathname.match(createURLPathMatchExp("form", baseUrl)) ? "active-tab-text" : ""}
                  `}
                >
                </i>
                <span 
                  className={`tab-text-padding 
                              ${pathname.match(createURLPathMatchExp("form", baseUrl)) ? "active-tab-text" : ""}`
                            }
                >
                  {t("Forms")}
                </span>
              </Nav.Link>
              {getUserRolePermission(userRoles, STAFF_DESIGNER) ? (
                <Nav.Link eventKey="10" as={Link} to={`${baseUrl}admin`}  className={`main-nav nav-item ${
                  pathname.match(createURLPathMatchExp("admin", baseUrl)) ? "" : "inactive-tab"
                }`}>
                  <i
                    className={`fa fa-user-circle-o fa-lg mr-2
                        ${pathname.match(createURLPathMatchExp("admin", baseUrl)) ? "active-tab-text" : ""}`
                    }
                  >
                  </i> 
                  <span 
                    className={`tab-text-padding 
                                ${pathname.match(createURLPathMatchExp("admin", baseUrl)) ? "active-tab-text" : ""}`
                              }
                  >
                    {t("Admin")}
                  </span>
                </Nav.Link>)
                :null}

              {getUserRolePermission(userRoles, STAFF_DESIGNER) ? (
                <Nav.Link
                  as={Link}
                  to={`${baseUrl}processes`}
                  className={`main-nav nav-item ${
                    pathname.match(
                      createURLPathMatchExp("processes", baseUrl)
                    )
                      ? "active-tab"
                      : ""
                  }`}
                >
                  <i className="fa fa-cogs fa-lg fa-fw mr-2" />
                  {t("Processes")}
                </Nav.Link>
              ) : null}                              

              {showApplications ? (
                getUserRolePermission(userRoles, STAFF_REVIEWER) ||  
                getUserRolePermission(userRoles, CLIENT)) ?
                <Nav.Link eventKey="2" as={Link} to={`${baseUrl}application`}  className={`main-nav nav-item ${
                  pathname.match(createURLPathMatchExp("application", baseUrl)) ? "" : "inactive-tab"
                }`}> 
                  <img
                    className={`applications-icon-header
                                ${pathname.match(createURLPathMatchExp("application", baseUrl)) ? "active-tab" : ""}
                                `}
                    src="/webfonts/fa-regular_list-alt.svg"
                    alt="Header Applications Icon"
                  /> 
                  <span
                    className={`tab-text-padding 
                    ${pathname.match(createURLPathMatchExp("application", baseUrl)) ? "active-tab-text" : ""}`
                  }
                  >
                    {t("Applications")}
                  </span>
                </Nav.Link>
                :
                null:
                null}

{/*              {getUserRolePermission(userRoles, STAFF_REVIEWER) ?
                <Nav.Link as={Link} to='/task'  className={`main-nav nav-item ${
                  pathname.match(/^\/task/) ? "active-tab" : ""
                }`}><i className="fa fa-list"/> Tasks</Nav.Link>
                :
                null}*/}

              {getUserRolePermission(userRoles, STAFF_REVIEWER) ?
                <NavDropdown
                  title={
                      <span className="white-text">
                        <img
                          className={`task-dropdown-icon
                                      ${pathname.match(createURLPathMatchExp("task", baseUrl)) ? "active-tab-dropdown" : "inactive-tab"}`
                                    }
                          src="/webfonts/fa-solid_list.svg"
                          alt="Task Icon"
                        /> 
                        <span
                          className={`tab-text-padding 
                                    ${pathname.match(createURLPathMatchExp("task", baseUrl)) ? "active-tab-text" : ""}
                          `}
                        >
                            {t("Tasks")}
                        </span>
                      </span>
                  }
                  id="task-dropdown"
                  className={`main-nav nav-item taskDropdown
                            ${pathname.match(createURLPathMatchExp("task", baseUrl)) ? "" : "inactive-tab"}`
                  }
                  onClick={goToTask}
                >
                  <ServiceFlowFilterListDropDown/>
              </NavDropdown>:null}

              {getUserRolePermission(userRoles, STAFF_REVIEWER) ?
                <NavDropdown 
                  title={
                  <span className="white-text">
                    <i 
                      class={`fa fa-tachometer fa-2 dashboard-icon-dropdown
                      ${pathname.match(createURLPathMatchExp("metrics", baseUrl)) || pathname.match(createURLPathMatchExp("insights", baseUrl)) ? "active-tab-text" : ""}
                      `}
                      aria-hidden="true"
                    >     
                    </i>
                    <span
                      className={`tab-text-padding 
                      ${pathname.match(createURLPathMatchExp("metrics", baseUrl)) || pathname.match(createURLPathMatchExp("insights", baseUrl)) ? "active-tab-text" : ""}
                          `}
                    >
                      {t("Dashboards")}
                    </span>
                  </span>
                  }
                  id="dashboard-dropdown"
                  className={`main-nav nav-item 
                              ${pathname.match(createURLPathMatchExp("metrics", baseUrl)) || pathname.match(createURLPathMatchExp("insights", baseUrl)) ? "" : "inactive-tab-dropdown"}`
                  }
                >
                  <NavDropdown.Item
                    eventKey="3"
                    as={Link}
                    to={`${baseUrl}metrics`}
                    className={`main-nav nav-item 
                    ${pathname.match(createURLPathMatchExp("metrics", baseUrl)) ? "dropdown-option-selected" : ""}`
                    }
                  >
                    <i
                      class={`fa fa-pie-chart dashboard-dropdown-options black-text
                      ${pathname.match(createURLPathMatchExp("metrics", baseUrl)) ? "dropdown-option-selected" : ""}
                      `}
                      aria-hidden="true"
                    >
                    </i>
                    <span className={`${pathname.match(createURLPathMatchExp("metrics", baseUrl)) ? "dropdown-option-selected" : "black-text"}`}>
                    {t("Metrics")}
                    </span>
                  </NavDropdown.Item>
                  {
                    getUserInsightsPermission() &&
                    <NavDropdown.Item
                      eventKey="4"
                      as={Link} to={`${baseUrl}insights`}
                      className={`main-nav nav-item 
                      ${pathname.match(createURLPathMatchExp("insights", baseUrl)) ? "dropdown-option-selected" : ""}
                      `}
                    >
                      <i
                        class={`fa fa-lightbulb-o dashboard-dropdown-options
                        ${pathname.match(createURLPathMatchExp("insights", baseUrl)) ? "dropdown-option-selected" : "black-text"}
                        `}
                        aria-hidden="true"
                      ></i>
                      <span className={`${pathname.match(createURLPathMatchExp("insights", baseUrl)) ? "dropdown-option-selected" : "black-text"}`}>
                        {t("Insights")}
                      </span>
                    </NavDropdown.Item>
                  }
                </NavDropdown>:null}
            </Nav>

            <Nav className="ml-lg-auto mr-auto px-lg-0 px-3">
                {selectLanguages.length === 1 ? (
                  selectLanguages.map((e, i) => {
                    return (
                      <>
                        <i className="fa fa-globe fa-lg mr-1 mt-1" />
                        <h4 key={i}>{e.name}</h4>
                      </>
                    );
                  })
                ) : (
                  <NavDropdown
                    title={
                      <>
                        <i className="fa fa-globe fa-lg mr-2" />
                        {lang ? lang : "LANGUAGE"}
                      </>
                    }
                    id="basic-nav-dropdown"
                  >
                    {selectLanguages.map((e, index) => (
                      <NavDropdown.Item
                        key={index}
                        onClick={() => {
                          handleOnclick(e.name);
                        }}
                      >
                        {e.value}{" "}
                      </NavDropdown.Item>
                    ))}
                  </NavDropdown>
                )}
              </Nav>

            <Nav className="ml-auto">
              {/* <Dropdown alignRight>
                  <Dropdown.Toggle id="dropdown-basic" as="div"> */}
                <NavDropdown id="dropdown-basic" as="div"  menuAlign="right">
                  <span className="mr-1">
                      <img
                        className="img-xs rounded-circle"
                        src="/assets/Images/user.svg"
                        alt="profile"
                      />
                    </span>
                      <span id="username" className="d-none d-lg-inline-block">
                      {user?.name || user?.preferred_username || ""}
                  </span>
              
                  <NavDropdown.Item disabled eventKey="5"> {user?.name || user?.preferred_username || ""}<br/>
                    <i className="fa fa-users fa-fw"/>
                    <b>{getUserRoleName(userRoles)}</b></NavDropdown.Item>
                  <NavDropdown.Divider/>
                  <NavDropdown.Item eventKey="6" as={Link} onClick ={logout}><i className="fa fa-sign-out fa-fw"/> {t("Logout")}{" "}</NavDropdown.Item>
                </NavDropdown>
            </Nav>

          </Navbar.Collapse>:<Link to={loginUrl} className="btn btn-primary">Login</Link>}
        </Container>
      </Navbar>
    </header>
  );
})

export default NavBar;