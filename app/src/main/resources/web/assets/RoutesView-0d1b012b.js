import{d as ie,C as ce,r as E,u as de,a3 as L,am as S,b$ as F,c0 as pe,ai as _e,J as me,aj as I,o as u,c as r,e,g as o,j as l,O as f,c2 as A,F as y,A as k,P as x,ak as ee,x as V,y as te,l as U,ap as ve,i as fe,t as ge,a7 as J,c3 as $e,c4 as he,N as be,R as ae,f as oe,S as ne,w as se,X as j,a8 as ye,$ as ke}from"./index-df35a132.js";import{_ as we}from"./Breadcrumb-120bdd32.js";import{T as m,a as w,_ as Ce,A as Ne}from"./question-mark-rounded-3eeccb5f.js";import{u as Te,a as Fe}from"./vee-validate.esm-aa1410cb.js";import"./stringToArray-a8dcbce9.js";const Ie={slot:"headline"},Ae={slot:"content"},Ee={class:"row mb-3"},Ve={class:"col-md-3 col-form-label"},De={class:"col-md-9"},Re=["value"],Me={key:0,class:"input-group mt-2"},Oe=["placeholder"],qe={class:"inner"},Se={class:"help-block"},Ue={value:""},Je=["value"],je={key:2,class:"invalid-feedback"},Le={class:"row mb-3"},Be={class:"col-md-3 col-form-label"},Pe={class:"col-md-9"},Qe=["value"],Xe={class:"row mb-3"},ze={class:"col-md-3 col-form-label"},Ge={class:"col-md-9"},He={value:"all"},Ke=["value"],We=["value"],Ye={class:"row mb-3"},Ze={class:"col-md-3 col-form-label"},xe={class:"col-md-9"},et={slot:"actions"},tt=["disabled"],le=ie({__name:"EditRouteModal",props:{data:{type:Object},devices:{type:Array},networks:{type:Array}},setup(h){var $,B,P,Q,X,z,G,H,K;const _=h,{handleSubmit:b}=Te(),i=ce({if_name:"",apply_to:"all",notes:"",target:"",is_enabled:!0}),p=E(m.INTERNET),D=Object.values(m).filter(s=>[m.IP,m.NET,m.REMOTE_PORT,m.INTERNET].includes(s)),{t:C}=de(),{mutate:R,loading:M,onDone:O}=L({document:S`
    mutation createConfig($input: ConfigInput!) {
      createConfig(input: $input) {
        ...ConfigFragment
      }
    }
    ${F}
  `,options:{update:(s,d)=>{pe(s,d.data.createConfig,S`
          query {
            configs {
              ...ConfigFragment
            }
          }
          ${F}
        `)}}}),{mutate:q,loading:n,onDone:N}=L({document:S`
    mutation updateConfig($id: ID!, $input: ConfigInput!) {
      updateConfig(id: $id, input: $input) {
        ...ConfigFragment
      }
    }
    ${F}
  `}),{value:c,resetField:g,errorMessage:v}=Fe("inputValue",_e().test("required",s=>"valid.required",s=>!w.hasInput(p.value)||!!s).test("target-value",s=>"invalid_value",s=>w.isValid(p.value,s??""))),a=($=_.data)==null?void 0:$.data;p.value=((P=(B=_.data)==null?void 0:B.target)==null?void 0:P.type)??m.INTERNET,c.value=((X=(Q=_.data)==null?void 0:Q.target)==null?void 0:X.value)??"",i.apply_to=((G=(z=_.data)==null?void 0:z.applyTo)==null?void 0:G.toValue())??"all",i.if_name=(a==null?void 0:a.if_name)??((K=(H=_.networks)==null?void 0:H[0])==null?void 0:K.ifName)??"",i.notes=(a==null?void 0:a.notes)??"",i.is_enabled=(a==null?void 0:a.is_enabled)??!0,a||g(),me(p,(s,d)=>{(s===m.INTERFACE||d===m.INTERFACE)&&(c.value="")});const T=b(()=>{const s=new w;s.type=p.value,s.value=c.value??"",i.target=s.toValue(),_.data?q({id:_.data.id,input:{group:"route",value:JSON.stringify(i)}}):R({input:{group:"route",value:JSON.stringify(i)}})});return O(()=>{I()}),N(()=>{I()}),(s,d)=>{var W,Y,Z;const ue=Ce,re=ve;return u(),r("md-dialog",null,[e("div",Ie,o(l(a)?s.$t("edit"):s.$t("create")),1),e("div",Ae,[e("div",Ee,[e("label",Ve,o(s.$t("traffic_to")),1),e("div",De,[f(e("select",{class:"form-select","onUpdate:modelValue":d[0]||(d[0]=t=>p.value=t)},[(u(!0),r(y,null,k(l(D),t=>(u(),r("option",{value:t},o(s.$t(`target_type.${t}`)),9,Re))),256))],512),[[A,p.value]]),l(w).hasInput(p.value)?(u(),r("div",Me,[f(e("input",{type:"text",class:"form-control","onUpdate:modelValue":d[1]||(d[1]=t=>ee(c)?c.value=t:null),placeholder:s.$t("for_example")+" "+l(w).hint(p.value)},null,8,Oe),[[x,l(c)]]),V(re,{class:"input-group-text"},{content:te(()=>[e("pre",Se,o(s.$t(`examples_${p.value}`)),1)]),default:te(()=>[e("span",qe,[V(ue)])]),_:1})])):U("",!0),p.value===l(m).INTERFACE?f((u(),r("select",{key:1,class:"form-select mt-2","onUpdate:modelValue":d[2]||(d[2]=t=>ee(c)?c.value=t:null)},[e("option",Ue,o(s.$t("all_local_networks")),1),(u(!0),r(y,null,k((W=h.networks)==null?void 0:W.filter(t=>t.type!=="wan"),t=>(u(),r("option",{value:t.ifName},o(t.name),9,Je))),256))],512)),[[A,l(c)]]):U("",!0),l(v)?(u(),r("div",je,o(l(v)?s.$t(l(v)):""),1)):U("",!0)])]),e("div",Le,[e("label",Be,o(l(C)("route_via")),1),e("div",Pe,[f(e("select",{class:"form-select","onUpdate:modelValue":d[3]||(d[3]=t=>i.if_name=t)},[(u(!0),r(y,null,k((Y=h.networks)==null?void 0:Y.filter(t=>["wan","vpn"].includes(t.type)),t=>(u(),r("option",{key:t.ifName,value:t.ifName},o(t.name),9,Qe))),128))],512),[[A,i.if_name]])])]),e("div",Xe,[e("label",ze,o(l(C)("apply_to")),1),e("div",Ge,[f(e("select",{class:"form-select","onUpdate:modelValue":d[4]||(d[4]=t=>i.apply_to=t)},[e("option",He,o(s.$t("all_devices")),1),(u(!0),r(y,null,k((Z=h.networks)==null?void 0:Z.filter(t=>!["wan","vpn"].includes(t.type)),t=>(u(),r("option",{key:t.ifName,value:"iface:"+t.ifName},o(t.name),9,Ke))),128)),(u(!0),r(y,null,k(h.devices,t=>(u(),r("option",{value:"mac:"+t.mac},o(t.name),9,We))),256))],512),[[A,i.apply_to]])])]),e("div",Ye,[e("label",Ze,o(l(C)("notes")),1),e("div",xe,[f(e("textarea",{class:"form-control","onUpdate:modelValue":d[5]||(d[5]=t=>i.notes=t),rows:"3"},null,512),[[x,i.notes]])])])]),e("div",et,[e("md-outlined-button",{value:"cancel",onClick:d[6]||(d[6]=(...t)=>l(I)&&l(I)(...t)),autofocus:""},o(s.$t("cancel")),1),e("md-filled-button",{value:"save",disabled:l(M)||l(n),onClick:d[7]||(d[7]=(...t)=>l(T)&&l(T)(...t))},o(s.$t("save")),9,tt)])])}}}),at={class:"page-container"},ot={class:"main"},nt={class:"v-toolbar"},st={class:"table-responsive"},lt={class:"table"},it=e("th",null,"ID",-1),dt={class:"actions two"},ut={class:"form-check"},rt=["disabled","onChange","checked"],ct={class:"nowrap"},pt={class:"nowrap"},_t={class:"actions two"},mt=["onClick"],vt=["onClick"],yt=ie({__name:"RoutesView",setup(h){const _=E([]),b=E([]),i=E([]),{t:p}=de();fe({handle:(n,N)=>{N?ge(p(N),"error"):(_.value=n.configs.filter(c=>c.group==="route").map(c=>{const g=JSON.parse(c.value),v=new Ne;v.parse(g.apply_to);const a=new w;return a.parse(g.target),{id:c.id,createdAt:c.createdAt,updatedAt:c.updatedAt,data:g,applyTo:v,target:a}}),b.value=[...n.devices],i.value=[...n.networks])},document:J`
    query {
      configs {
        ...ConfigFragment
      }
      devices {
        ...DeviceFragment
      }
      networks {
        ...NetworkFragment
      }
    }
    ${$e}
    ${F}
    ${he}
  `});function D(n){j(ye,{id:n.id,name:n.id,gql:J`
      mutation DeleteConfig($id: ID!) {
        deleteConfig(id: $id)
      }
    `,appApi:!1,typeName:"Config"})}function C(n){j(le,{data:n,devices:b,networks:i})}function R(){j(le,{data:null,devices:b,networks:i})}const{mutate:M,loading:O}=L({document:J`
    mutation updateConfig($id: ID!, $input: ConfigInput!) {
      updateConfig(id: $id, input: $input) {
        ...ConfigFragment
      }
    }
    ${F}
  `});function q(n){M({id:n.id,input:{group:"route",value:JSON.stringify(n.data)}})}return(n,N)=>{const c=we,g=ke,v=be("tooltip");return u(),r("div",at,[e("div",ot,[e("div",nt,[V(c,{current:()=>n.$t("page_title.routes")},null,8,["current"]),e("button",{type:"button",class:"btn right-actions",onClick:R},o(n.$t("create")),1)]),e("div",st,[e("table",lt,[e("thead",null,[e("tr",null,[it,e("th",null,o(n.$t("apply_to")),1),e("th",null,o(n.$t("description")),1),e("th",null,o(n.$t("notes")),1),e("th",null,o(n.$t("enabled")),1),e("th",null,o(n.$t("created_at")),1),e("th",null,o(n.$t("updated_at")),1),e("th",dt,o(n.$t("actions")),1)])]),e("tbody",null,[(u(!0),r(y,null,k(_.value,a=>{var T;return u(),r("tr",{key:a.id},[e("td",null,[V(g,{id:a.id,raw:a.data},null,8,["id","raw"])]),e("td",null,o(a.applyTo.getText(n.$t,b.value,i.value)),1),e("td",null,o(n.$t("route_description",{if_name:((T=i.value.find($=>$.ifName==a.data.if_name))==null?void 0:T.name)??a.data.if_name,target:a.target.getText(n.$t,i.value)})),1),e("td",null,o(a.notes),1),e("td",null,[e("div",ut,[e("md-checkbox",{"touch-target":"wrapper",disabled:l(O),onChange:$=>q(a),checked:a.data.is_enabled},null,40,rt)])]),e("td",ct,[f((u(),r("span",null,[oe(o(l(ne)(a.createdAt)),1)])),[[v,l(ae)(a.createdAt)]])]),e("td",pt,[f((u(),r("span",null,[oe(o(l(ne)(a.updatedAt)),1)])),[[v,l(ae)(a.updatedAt)]])]),e("td",_t,[e("a",{href:"#",class:"v-link",onClick:se($=>C(a),["prevent"])},o(n.$t("edit")),9,mt),e("a",{href:"#",class:"v-link",onClick:se($=>D(a),["prevent"])},o(n.$t("delete")),9,vt)])])}),128))])])])])])}}});export{yt as default};
