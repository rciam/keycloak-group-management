/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import * as React from 'react';
import {Route, Switch,matchPath} from 'react-router-dom';
import {NavItem, NavExpandable} from '@patternfly/react-core';
import {Msg} from './widgets/Msg';
import {PageNotFound} from './content/page-not-found/PageNotFound';
import { ForbiddenPage } from './content/forbidden-page/ForbiddenPage';
import {GroupPage} from './content/group-management-pages/GroupPage';

export interface ContentItem {
    id?: string;
    label: string;
    labelParams?: string[];
    hidden?: string;
    groupId: string; // computed value
    itemId: string; // computed value
};

let customPages =[ 
    {
        path: "/group-managment/showgroups/:id",
        expandId: "group-management",
        parentId: "showgroups",
        componentName: "GroupPage"
    }
]

export interface Expansion extends ContentItem {
    content: ContentItem[];
}

export interface PageDef extends ContentItem {
    path: string;
}

export interface ComponentPageDef extends PageDef {
    component: React.ComponentType;
}

export interface ModulePageDef extends PageDef {
    modulePath: string;
    componentName: string;
    module: React.Component; // computed value
}

export function isModulePageDef(item: ContentItem): item is ModulePageDef {
    return (item as ModulePageDef).modulePath !== undefined;
}

export function isExpansion(contentItem: ContentItem): contentItem is Expansion {
    return (contentItem as Expansion).content !== undefined;
}

declare const content: ContentItem[];

function groupId(group: number): string {
    return 'grp-' + group;
}

function itemId(group: number, item: number): string {
    return 'grp-' + group + '_itm-' + item;
}

function isChildOf(parent: Expansion, child: PageDef): boolean {
    for (var item of parent.content) {
        if (isExpansion(item) && isChildOf(item, child)) return true;
        if (parent.groupId === child.groupId) return true;
    }

    return false;
}

function createNavItems(activePage: PageDef, contentParam: ContentItem[], groupNum: number): React.ReactNode {
    if (typeof content === 'undefined') return (<React.Fragment/>);
        let current_path = window.location.hash.substring(1);
        let customPage = {
            path:"",
            parentId:"",
            expandId:"",
            componentName:""
        }
        customPages.forEach(page=>{
            matchPath(current_path, {
                path: page.path,
                exact: true,
                strict: false
              })&&(customPage=page)&&(activePage={path:"",label:"",groupId:"",itemId:""});
        })

        const links: React.ReactElement[] = contentParam.map((item: ContentItem) => {
            console.log(item);
            const navLinkId = `nav-link-${item.id}`;
            if (isExpansion(item)) {
                return <NavExpandable id={navLinkId}
                                        groupId={item.groupId}
                                        key={item.groupId}
                                        title={Msg.localize(item.label, item.labelParams)}
                                        isExpanded={isChildOf(item, activePage)||customPage.expandId===item.id}
                                        >
                            {createNavItems(activePage, item.content, groupNum + 1)}
                        </NavExpandable>
            } else {
                const page: PageDef = item as PageDef;
                return <NavItem id={navLinkId}
                                groupId={item.groupId}
                                itemId={item.itemId}
                                key={item.itemId}
                                to={'#/' + page.path}
                                isActive={activePage.itemId === item.itemId||customPage.parentId===item.id}
                                type="button">
                            {Msg.localize(page.label, page.labelParams)}
                        </NavItem>
            }
    });

    return (<React.Fragment>{links}</React.Fragment>);
}

export function makeNavItems(activePage: PageDef): React.ReactNode {
    return createNavItems(activePage, content, 0);
}

function setIds(contentParam: ContentItem[], groupNum: number): number {
    if (typeof contentParam === 'undefined') return groupNum;
    let expansionGroupNum = groupNum;

    for (let i = 0; i < contentParam.length; i++) {
        const item: ContentItem = contentParam[i];
        if (isExpansion(item)) {
            item.itemId = itemId(groupNum, i);
            expansionGroupNum = expansionGroupNum + 1;
            item.groupId = groupId(expansionGroupNum);
            expansionGroupNum = setIds(item.content, expansionGroupNum);
            console.log('currentGroup=' + (expansionGroupNum));
        } else {
            item.groupId = groupId(groupNum);
            item.itemId = itemId(groupNum, i);
        }
    };

    return expansionGroupNum;
}

export function initGroupAndItemIds(): void {
    setIds(content, 0);
    console.log({content});
}

// get rid of Expansions and put all PageDef items into a single array
export function flattenContent(pageDefs: ContentItem[]): PageDef[] {
    const flat: PageDef[] = [];

    for (let item of pageDefs) {
        if (isExpansion(item)) {
            flat.push(...flattenContent(item.content));
        } else {
            flat.push(item as PageDef);
        }
    }

    return flat;
}

export function makeRoutes(): React.ReactNode {
    if (typeof content === 'undefined') return (<span/>);
    const customComponents = {
        GroupPage:GroupPage
    }
    const pageDefs: PageDef[] = flattenContent(content);

    const routes: React.ReactElement<Route>[] = pageDefs.map((page: PageDef) => {
        if (isModulePageDef(page)) {
            const node: React.ReactNode = React.createElement(page.module[page.componentName], {'pageDef': page});
            return <Route key={page.itemId} path={'/' + page.path} exact render={() => node} />;
        } else {
            const pageDef: ComponentPageDef = page as ComponentPageDef;
            return <Route key={page.itemId} path={'/' + page.path} exact component={pageDef.component}/>;
        }
    });

    return (<Switch>
                {routes}
                {customPages.map((item,index)=>{
                    return <Route path={item.path} component={customComponents[item.componentName]}/>
                })}
                <Route path="/group-managment/showgroups/:id" component={GroupPage}/>
                <Route path="/forbidden" component={ForbiddenPage}/>
                <Route component={PageNotFound}/>
            </Switch>);
}
